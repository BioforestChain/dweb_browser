<script lang="ts" setup>


function focusin(){
  // 添加监听 entry
  // 跳转到指定页面
}

function focusout(){
  // 删除监听 entry
}

</script>
<template>
  <div :class="$style.container">
    <input 
      :class="$style.input" 
      @focusout="focusout"
      @focusin="focusin"
      placeholder="请使用键盘输入！！entry确定"
    />
  </div>
</template>
<style module>
.container{
  position: absolute;
  left: 0px;
  bottom: 0px;
  display: flex;
  flex-grow: 0;
  flex-shrink: 0;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 45px;
  background: #999999;
}

.input{
  box-sizing: border-box;
  padding: 0px 8px;
  width: 80%;
  line-height: 38px;
  border-radius: 5px;
  background: #fff;
}

</style>
