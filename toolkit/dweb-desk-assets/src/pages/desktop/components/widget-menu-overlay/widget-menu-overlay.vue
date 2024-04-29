<script setup lang="ts">
import MenuBox from "@/components/menu-box/menu-box.vue";
import { shallowRef, triggerRef } from "vue";
const props = defineProps({
  showReason: {
    type: Object,
    default: defaultReason,
  },
  show: {
    type: Boolean,
  },
  zIndex: {
    type: Number,
    default: 2,
  },
});
const emits = defineEmits<{
  (event: "close"): void;
}>();
// watch(props, () => {
//   showOverlay(props.showReason, props.show);
// });
const beforeEnter = () => {
  console.log("beforeEnter");
};
const afterLeave = () => {
  console.log("afterLeave");
  /// 在动画结束后，需要将lastEffectReason的引用给释放掉
  // overlayReasons.value.delete(lastEffectedReason.value);
  // lastEffectedReason.value = defaultReason;
  // triggerRef(overlayReasons);
};
</script>
<script lang="ts">
const defaultReason = {};
const overlayReasons = shallowRef(new Set<{}>());
/// 保留最后一个改变overlayReasons 的 reason，这样动画可以保持与最后一个元素进行交互
const lastEffectedReason = shallowRef(defaultReason);
export const ownReason = (reason: {}) => {
  // console.log("ownReason", overlayReasons.value.has(reason), lastEffectedReason.value === reason);
  return overlayReasons.value.has(reason) || lastEffectedReason.value === reason;
};
export const showOverlay = (reason: {}, toggle = true) => {
  // console.log("showOverlay", toggle);
  if (toggle) {
    if (overlayReasons.value.has(reason) === false) {
      overlayReasons.value.add(reason);
      lastEffectedReason.value = reason;
    }
  } else {
    if (overlayReasons.value.delete(reason)) {
      lastEffectedReason.value = reason;
    }
  }
  triggerRef(overlayReasons);
};
</script>
<template>
  <Transition name="fade" @beforeEnter="beforeEnter" @afterLeave="afterLeave">
    <MenuBox
      class="overlay ios-ani"
      v-if="overlayReasons.size"
      @click="emits('close')"
      @menu="emits('close')"
    ></MenuBox>
  </Transition>
</template>
<style scoped lang="scss">
.overlay {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  z-index: v-bind("zIndex");
  // pointer-events: none;
  // background-color: rgba(0, 0, 0, 0.3);

  backdrop-filter: blur(10px);

  // .glass-material-overlay {
  //   backdrop-filter: blur(20px);
  //   width: 100%;
  //   height: 100%;
  // }

  &.fade-enter-from,
  &.fade-leave-to {
    opacity: 0;
  }
  &.fade-leave-to {
    pointer-events: none;
  }
}
</style>
