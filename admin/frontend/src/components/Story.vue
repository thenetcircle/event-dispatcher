<template>

  <div class="container">
    <nav class="breadcrumb has-succeeds-separator" aria-label="breadcrumbs">
      <ul>
        <li>
          <router-link :to="{ name: 'home' }">Home</router-link>
        </li>
        <li>
          <router-link :to="{ name: 'stories' }">Stories</router-link>
        </li>
        <li class="is-active"><a href="#" aria-current="page">Story Details</a></li>
      </ul>
    </nav>

    <div class=" box story" style="margin-bottom: 1.5rem">

      <div class="level">
        <div class="level-left">
          <div class="level-item">
            <p class="title is-2 is-spaced story-name">{{ storyName }}</p>
          </div>
          <!--<div class="level-item">
            <div class="tags has-addons">
              <span class="tag is-dark">Status:</span>
              <span class="tag is-info">{{ storyInfo.status }}</span>
            </div>
          </div>-->
        </div>
      </div>

      <div class="tabs is-medium">
        <ul>
          <li :class="{ 'is-active': isOverview }" @click="changeTab('overview')"><a>Overview</a>
          </li>
          <li :class="{ 'is-active': isRunners }" @click="changeTab('runners')"><a>Runners</a></li>
          <li :class="{ 'is-active': isMonitoring }" @click="changeTab('monitoring')">
            <a>Statistics</a></li>
        </ul>
      </div>

      <div v-if="isOverview">

        <story-graph v-if="storyInfo.source !== undefined" :info="storyInfo"
                     @save="onSaveStory"></story-graph>

      </div>

      <div v-if="isRunners">

        <div class="level">
          <div class="level-left">
          </div>
          <div class="level-right">
            <div class="level-item">
              <a class="button" @click="onOpenChooseRunner">Assign to a new runner</a>
            </div>
          </div>
        </div>

        <table class="table is-bordered is-striped is-narrow is-hoverable is-fullwidth">
          <thead>
          <tr>
            <th>Name</th>
            <th>Server</th>
            <th>Version</th>
            <th>Amount</th>
            <th>Action</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="item in runners">
            <td>
              <router-link :to="{ name: 'runner', params: { 'runnerName': item.name } }">{{
                item.name }}
              </router-link>
            </td>
            <td>{{ item.host }}</td>
            <td>{{ item.version }}</td>
            <td>{{ item.stories[storyName] }}</td>
            <td>
              <a class="button is-primary" @click="onRerun(item)">Rerun</a>
              <a class="button is-danger" @click="onDismissRunner(item)">Dismiss</a>
            </td>
          </tr>
          </tbody>
        </table>

        <choose-runner v-if="chooseRunner" @close="onCloseChooseRunner" @choose="onAssignRunner"
                       :excludes="runners.map(info => info.name)"></choose-runner>

      </div>

      <div v-if="isMonitoring">
        <story-statistics :storyname="storyName"></story-statistics>
      </div>


    </div>
  </div>

</template>

<script lang="ts">
  import Vue from "vue"
  import request from '../lib/request'
  import {StoryInfo, StoryUtils} from '../lib/story-utils'
  import StoryGraph from './StoryGraph.vue'
  import GrafanaGraph from './GrafanaGraph.vue'
  import {RunnerInfo} from '../lib/runner-utils'
  import ChooseRunner from './ChooseRunner.vue'
  import StoryStatistics from './StoryStatistics.vue'
  import bus from '../lib/bus'

  let tabRouteMapping: { [key: string]: string } = {
    'overview': 'story',
    'monitoring': 'story-statistics',
    'runners': 'story-runners'
  }

  export default Vue.extend({
    data() {
      return {
        storyName: this.$route.params.storyName,
        storyInfo: {} as StoryInfo,
        // currTab: 'overview',
        runners: <RunnerInfo[]>[],
        chooseRunner: false
      }
    },

    components: {
      StoryGraph,
      GrafanaGraph,
      ChooseRunner,
      StoryStatistics
    },

    created() {
      this.fetchData()
    },

    watch: {
      '$route': 'fetchData'
    },

    methods: {
      fetchData() {
        request.getStory(this.storyName)
          .then(storyInfo => {
            this.storyInfo = storyInfo
          })
        request.getStoryRunners(this.storyName)
          .then(infos => {
            this.runners = infos
          })
      },

      changeTab(tab: string) {
        let routeName = tabRouteMapping[tab]
        this.$router.push({name: routeName, params: {storyName: this.storyName}})
        // this.currTab = tab
      },

      onSaveStory(newStoryInfo: StoryInfo) {
        request.updateStory(this.storyName, newStoryInfo)
          .then(() => {
            this.storyInfo = StoryUtils.copyStoryInfo(newStoryInfo)
            bus.$emit('notify', 'The story has been updated!')
          })
      },

      onOpenChooseRunner(): void {
        this.chooseRunner = true
      },
      onCloseChooseRunner(): void {
        this.chooseRunner = false
      },
      onAssignRunner(runnerInfo: RunnerInfo): void {
        let typpedAmount = prompt(`How many instances you want the runner "${runnerInfo.name}" to run?`, '1')
        // confirm(`Are you sure to assign story "${this.storyName}" to runner "${runnerInfo.name}"?`)
        if (typpedAmount != null && typpedAmount != "") {
          let amount = parseInt(typpedAmount)
          if (isNaN(amount) || amount <= 1) amount = 1
          runnerInfo.stories[this.storyName] = amount + ''
          request.assignStory(runnerInfo.name, this.storyName, amount)
            .then(() => {
              this.runners.push(runnerInfo)
              this.onCloseChooseRunner()
              bus.$emit('notify', `The story has been assigned to runner "${runnerInfo.name}"!`)
            })
        }
      },
      onDismissRunner(runnerInfo: RunnerInfo): void {
        if (confirm(`Are you sure to dismiss story "${this.storyName}" from runner "${runnerInfo.name}"?`)) {
          request.unassignStory(runnerInfo.name, this.storyName)
            .then(() => {
              let index = -1
              this.runners.forEach((info, _index) => {
                if (info.name == runnerInfo.name) {
                  index = _index
                  return;
                }
              })
              if (index !== -1) {
                this.runners.splice(index, 1)
              }
              bus.$emit('notify', `The story has been unassigned from runner "${runnerInfo.name}"!`)
            })
        }
      },
      onRerun(runnerInfo: RunnerInfo): void {
        if (confirm(`Are you sure to rerun story "${this.storyName}" on runner "${runnerInfo.name}"?`)) {
          request.rerunStory(runnerInfo.name, this.storyName)
            .then(() => {
              bus.$emit('notify', `The story has been rerun on runner "${runnerInfo.name}"!`)
            })
        }
      }
    },

    computed: {
      currTab(): string {
        for (let key in tabRouteMapping) {
          if (tabRouteMapping[key] == this.$route.name)
            return key;
        }
        return "overview";
      },
      isOverview(): boolean {
        return this.currTab == 'overview'
      },
      isMonitoring(): boolean {
        return this.currTab == 'monitoring'
      },
      isRunners(): boolean {
        return this.currTab == 'runners'
      }
    }
  })
</script>

<style>
  .story {
    background-color: white;
    background-image: -webkit-linear-gradient(top, white, #efefef, #fefefe, #fefefe, #fefefe, white);
    background-image: -moz-linear-gradient(top, white, #efefef, #fefefe, #fefefe, #fefefe, white);
    background-image: -o-linear-gradient(top, white, #efefef, #fefefe, #fefefe, #fefefe, white);
    background-image: linear-gradient(to bottom, #fefefe, #efefef, #fefefe, #fefefe, #fefefe, white);
  }

  .story-name {
    padding: 15px 20px;
  }

  .delimiter {
    padding-top: 50px;
  }

  .content {
    font-size: 0.9rem;
  }

  pre.settings {
    width: 100%;
  }
</style>
