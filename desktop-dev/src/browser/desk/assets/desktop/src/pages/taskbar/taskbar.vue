<script setup lang="ts">

</script>
<template>
  <div class="taskbar">
    <div class="panel">
      <div class="app-icon">
        <img class="img" src="${icons.anquanzhongxin}" draggable="false" />
      </div>
      <div class="app-icon">
        <img class="img" src="${icons.kandianying}" draggable="false" />
      </div>
      <div class="app-icon">
        <img class="img" src="${icons.naozhong}" draggable="false" />
      </div>
      <div class="app-icon">
        <img class="img" src="${icons.xiangji}" draggable="false" />
      </div>
      <hr class="divider" />

      <div class="app-icon" @click="${this._open_desktop}">
        <img class="img" src="${icons.quanbufenlei}" draggable="false" />
      </div>
    </div>
  </div>
</template>
<style scoped lang="scss">
.taskbar {
  display: block;
  height: min-content;
  width: min-content;
  -webkit-app-region: drag;
  cursor: move;
  user-select: none;
}
.panel {
  padding: 1em;
  display: flex;
  flex-direction: column;
  gap: 1em;
}
.divider {
  width: 100%;
  height: 1px;
  border-radius: 1px;
  border: 0;
  background: linear-gradient(to right, transparent, currentColor, transparent);
}
.app-icon {
  cursor: pointer;
  -webkit-app-region: no-drag;
  width: 60px;
  height: 60px;
  border-radius: 15px;
  background-color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0px 3px 5px rgba(0, 0, 0, 0.3);
  .img {
    width: 90%;
    height: auto;
  }
}
</style>
