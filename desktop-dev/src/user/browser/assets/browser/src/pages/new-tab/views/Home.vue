<script lang="ts" setup>
// import jmm from "../components/JMM.vue";
</script>
<template>
  <div class="container">
    <!-- <jmm></jmm> -->
    <div class="logo">
      <img src="@/assets/logo.svg" alt="Dweb Browser" class="icon" />
      <div class="gradient-text">Dweb Browser</div>
    </div>
  </div>
</template>
<style scoped>
.container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  width: 100%;
  padding: 10px;
}

.logo {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.icon {
  width: 210px;
  height: 210px;
  border-radius: 0px 0px 0px 0px;
  opacity: 1;
}
.gradient-text {
  width: 150px;
  height: 20px;
  font-size: 20px;
  font-family: Source Han Sans CN-Medium, Source Han Sans CN;
  font-weight: 500;
  color: #0a1626;
  line-height: 20px;
  display: flex;
  justify-content: center;
}
</style>
