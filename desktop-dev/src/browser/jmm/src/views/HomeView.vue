<script setup lang="ts">
import type { $JmmAppInstallManifest } from "&/types.ts";
import Detail from "@/components/Detail.vue";
import Header from "@/components/Header.vue";
import ImageDetail from "@/components/Image.vue";
import { computed, onMounted, ref, type Ref } from "vue";




const metadata: Ref<$JmmAppInstallManifest> = ref({} as $JmmAppInstallManifest);
const metaUrl = new URLSearchParams(location.search).get("metadataUrl");

onMounted(async () => {
  if (!metaUrl) {
    throw new Error("miss params: metadataUrl");
  }
  const data = await fetch(metaUrl);
  metadata.value = await data.json();
  console.log(metadata.value);
});

const author = computed(() => {
  if (!metadata.value.author) {
    return "anonymous"
  }
  if(metadata.value.author instanceof Array) {
    return metadata.value.author[0]
  }
  return metadata.value.author
})

const lang = computed(() => {
  if (!metadata.value.lang || metadata.value.lang == "") return "EN";
  return metadata.value.lang;
});

const lenguagesLen = computed(() => {
  const len = metadata.value.languages?.length ?? 0;
  if (len === 0) return "+1 More";
  return `+${len} More`;
});
const mb = computed(() => {
  return ((metadata.value.bundle_size / 1024) / 1024).toFixed(2);
})
</script>

<template>
  <div class="container">
    <Header :app-info="metadata" :metadataUrl="metaUrl!" />
    <div class="divider"></div>
    <div class="suport">
      <div class="item">
        <div class="title">SIZE</div>
        <div class="icon"><svg t="1692958421186" class="icon-img" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="7534" width="200" height="200"><path d="M809.550222 770.936242v75.876849c0 20.565333-16.588283 37.210505-37.004929 37.210505H251.461172c-20.368808 0-37.011394-16.682667-37.011394-37.193697v-33.22699H177.254788v33.22699c0 41.010424 33.19596 74.287838 74.207677 74.387394h521.082828a74.269737 74.269737 0 0 0 52.514909-21.829818 74.245172 74.245172 0 0 0 21.683717-52.575677V770.934949h-37.193697z" fill="#3D3D3D" p-id="7535"></path><path d="M870.617859 474.479192v272.076283H153.380848V474.479192h717.237011m5.052767-37.016566H148.330667C130.675717 437.462626 116.36299 451.771475 116.36299 469.426424V751.605657c0 17.653657 14.310141 31.967677 31.967677 31.967676h727.341252c17.648485 0 31.965091-14.310141 31.965091-31.965091v-282.181818c-0.001293-17.654949-14.316606-31.963798-31.966384-31.963798z" fill="#3D3D3D" p-id="7536"></path><path d="M866.20897 455.365818H157.789737c-8.568242 0-15.515152 6.946909-15.515151 15.515152v279.272727c0 8.568242 6.946909 15.515152 15.515151 15.515151h708.419233c8.568242 0 15.515152-6.946909 15.515151-15.515151v-279.272727c0-8.568242-6.946909-15.515152-15.515151-15.515152zM465.42804 726.42198H278.880323v-37.118707l112.443475-140.58796a433.014949 433.014949 0 0 1 10.750707-12.931879h-110.663111v-41.168161h170.477899v36.110222l-123.248485 152.295434-1.927757 2.230303h128.713696v41.170748z m64.283152 0H485.442586v-231.809293h44.268606v231.809293z m195.798626-109.500768c-13.376646 14.553212-36.000323 21.629414-69.166545 21.629414h-47.603071v87.871354h-44.269899v-231.809293h89.36598c14.951434 0 26.163717 0.716283 34.280727 2.186343 11.588525 1.930343 21.633293 5.76 29.73996 11.36097 8.260525 5.71604 14.956606 13.746424 19.905939 23.867475 4.880808 9.990465 7.355475 21.068283 7.355475 32.929616 0 20.323556-6.596525 37.805253-19.608566 51.964121z" fill="#3D3D3D" p-id="7537"></path><path d="M680.795152 537.416404c-2.770747-0.734384-9.358222-1.634263-24.59798-1.634263h-47.45697v61.593859h48.045253c16.418909 0 27.916929-2.810828 34.174707-8.35103 6.061253-5.370828 9.005253-12.956444 9.005252-23.182223 0-7.447273-1.77002-13.557657-5.412202-18.686707-3.523232-4.966141-8.023919-8.153212-13.75806-9.739636z" fill="#3D3D3D" p-id="7538"></path><path d="M518.894545 53.607434H251.461172c-40.934141 0-74.207677 33.307152-74.207677 74.387394V444.767677h37.19499V127.994828c0-20.552404 16.628364-37.196283 37.011394-37.196283h267.433374V53.607434zM604.989414 53.607434H567.789253v241.908364c0 20.417939 16.593455 37.043717 37.047595 37.043717h204.712081V438.30303h37.19499V295.365818L604.989414 53.607434z m-0.391757 241.908364l0.391757-189.315879 189.199515 189.315879H604.597657z" fill="#3D3D3D" p-id="7539"></path><path d="M519.256566 72.177778m-18.570344 0a18.570343 18.570343 0 1 0 37.140687 0 18.570343 18.570343 0 1 0-37.140687 0Z" fill="#3D3D3D" p-id="7540"></path></svg></div>
        <div class="message">{{ mb }}MB</div>
      </div>
      <div class="item">
        <div class="title">DEVELOPER</div>
        <div class="icon"><svg t="1692958144346" class="icon-img" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="4360" width="200" height="200"><path d="M0 0h1024v1024H0z" fill="#FFFFFF" fill-opacity="0" p-id="4361"></path><path d="M722.908 613.998c34.142 9.148 54.403 44.242 45.255 78.384l-72.21 269.493c-34.143-9.148-54.404-44.242-45.256-78.384l72.211-269.493z m123.003 32.595L961.876 762.56c18.558 18.558 18.743 48.53 0.557 67.316l-0.557 0.566-115.965 115.966c-24.994-24.994-24.994-65.517 0-90.51l59.396-59.398-59.396-59.396c-24.994-24.994-24.994-65.516 0-90.51z m-274.255 0c24.993 24.994 24.993 65.516 0 90.51L512.259 796.5l59.397 59.397c24.743 24.744 24.99 64.707 0.742 89.755l-0.742 0.755L455.69 830.44c-18.557-18.558-18.743-48.53-0.556-67.316l0.556-0.566 115.966-115.966zM512 513l6.231 0.048c68.977 1.056 133.708 19.615 189.948 51.431-26.08 11.18-46.98 33.656-55.157 62.97l-0.244 0.891-2.433 9.055a96.27 96.27 0 0 0-10.278-12.625l-0.829-0.849-0.383-0.385-1.122-1.105c-36.892-35.714-95.531-35.71-132.418 0.011l-1.111 1.094-93.952 93.951-0.92 0.936-1.443 1.511c-40.813 43.535-40.289 111.565 1.274 154.47l1.272 1.292L446.74 912H146.337l-0.486-0.004c-17.429-0.283-31.47-14.5-31.47-31.996 0-1.49 0.101-2.957 0.298-4.393l-0.037-0.045C133.045 672.278 303.922 513 512 513z m0.5-449C636.488 64 737 164.512 737 288.5S636.488 513 512.5 513 288 412.488 288 288.5 388.512 64 512.5 64z" fill="#666666" p-id="4362"></path></svg></div>
        <div class="message">{{ author }}</div>
      </div>
      <div class="item">
        <div class="title">LENGUAGES</div>
        <div class="icon">{{ lang }}</div>
        <div class="message">{{ lenguagesLen }}</div>
      </div>
    </div>
    <div class="divider"></div>
    <div class="change">
      <div class="title">
        <div class="top">更新信息</div>
        <div class="version">version: {{ metadata.version }}</div>
      </div>
      <div class="changeLog">{{ metadata.change_log }}</div>
    </div>
    <ImageDetail :images="metadata.images"></ImageDetail>
    <div class="divider"></div>
    <Detail :description="metadata.description"></Detail>
  </div>
</template>

<style scoped lang="scss">
.container {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  .divider {
    height: 1px;
    background: linear-gradient(to right, transparent, #ccc, transparent);
  }

  .suport {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    padding: 20px 12px;
    .item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 10px;
      font-family:Arial,Helvetica,sans-serif;font-size:100%;
      color: #959595;
      border-radius: 4px;
      .title {
        font-size: 12px;
        font-weight: bold;
      }

      .icon {
        width: 40px;
        height: 40px;
        line-height: 40px;
        font-size: 20px;
        text-align: center;
        border-radius: 50%;
        background: #eee;
        margin: 2px 0;
        .icon-img {
          width: 35px;
          height: 40px;
        }
      }

      .message {
        padding: 3px 10px;
        border-radius: 3px;
      }
    }
  }

  .change {
    display: flex;
    flex-direction: column;
    padding: 20px 12px;
    .title {
      display: flex;
      align-items: center;
      justify-content: space-between;
      .top {
        font-size: 20px;
        font-weight: 400;
        margin: 10px 0;
      }
    }
  }
}
</style>
