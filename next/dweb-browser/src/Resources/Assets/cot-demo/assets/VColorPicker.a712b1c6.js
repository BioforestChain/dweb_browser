var _a;
import { k as includes, l as defineComponent, r as ref, m as computed, p as convertToUnit, q as useResizeObserver, s as watch, t as clamp, o as onMounted, x as useRender, a as createVNode, y as getEventCoordinates, z as propsFactory, A as getCurrentInstanceName, j as isRef, B as destructComputed, C as isCssColor, F as Fragment, u as unref, D as makeTagProps, E as makeThemeProps, G as genericComponent, H as provideTheme, I as provideDefaults, J as toRef, K as pick, L as getCurrentInstance, M as getUid, N as provide, O as inject, P as onBeforeUnmount, Q as reactive, R as useProxiedModel, S as deepEqual, U as wrapInArray, V as findChildrenWithProvide, W as consoleWarn, X as mergeProps, Y as toRefs, Z as IconValue, _ as Text, $ as useIcon, a0 as SUPPORTS_INTERSECTION, a1 as watchEffect, a2 as isObject, a3 as keyCodes, a4 as useRtl, a5 as resolveDynamicComponent, a6 as hasEvent, a7 as nextTick, w as withDirectives, a8 as resolveDirective, a9 as HSVtoRGB, aa as RGBtoHSV, ab as HSVtoHSL, ac as HSLtoHSV, ad as HSVtoHex, ae as HexToHSV, af as parseHex, T as TransitionGroup, ag as Transition, ah as h, ai as camelize, aj as onBeforeMount, ak as useToggleScope, al as useLocale, am as EventProp, an as isOn, ao as createRange, ap as vShow, aq as keyValues, ar as HSVtoCSS, as as getContrast } from "./index.f89085c2.js";
const block = ["top", "bottom"];
const inline = ["start", "end", "left", "right"];
function parseAnchor(anchor, isRtl) {
  let [side, align] = anchor.split(" ");
  if (!align) {
    align = includes(block, side) ? "start" : includes(inline, side) ? "top" : "center";
  }
  return {
    side: toPhysical(side, isRtl),
    align: toPhysical(align, isRtl)
  };
}
function toPhysical(str, isRtl) {
  if (str === "start")
    return isRtl ? "right" : "left";
  if (str === "end")
    return isRtl ? "left" : "right";
  return str;
}
var VColorPicker$1 = "";
var VColorPickerCanvas$1 = "";
const VColorPickerCanvas = defineComponent({
  name: "VColorPickerCanvas",
  props: {
    color: {
      type: Object
    },
    disabled: Boolean,
    dotSize: {
      type: [Number, String],
      default: 10
    },
    height: {
      type: [Number, String],
      default: 150
    },
    width: {
      type: [Number, String],
      default: 300
    }
  },
  emits: {
    "update:color": (color) => true,
    "update:position": (hue) => true
  },
  setup(props, _ref) {
    let {
      emit
    } = _ref;
    const isInteracting = ref(false);
    const isOutsideUpdate = ref(false);
    const dotPosition = ref({
      x: 0,
      y: 0
    });
    const dotStyles = computed(() => {
      const {
        x,
        y
      } = dotPosition.value;
      const radius = parseInt(props.dotSize, 10) / 2;
      return {
        width: convertToUnit(props.dotSize),
        height: convertToUnit(props.dotSize),
        transform: `translate(${convertToUnit(x - radius)}, ${convertToUnit(y - radius)})`
      };
    });
    const canvasRef = ref();
    const canvasWidth = ref(parseFloat(props.width));
    const canvasHeight = ref(parseFloat(props.height));
    const {
      resizeRef
    } = useResizeObserver((entries) => {
      var _a2;
      if (!((_a2 = resizeRef.value) == null ? void 0 : _a2.offsetParent))
        return;
      const {
        width,
        height
      } = entries[0].contentRect;
      canvasWidth.value = width;
      canvasHeight.value = height;
    });
    function updateDotPosition(x, y, rect) {
      const {
        left,
        top,
        width,
        height
      } = rect;
      dotPosition.value = {
        x: clamp(x - left, 0, width),
        y: clamp(y - top, 0, height)
      };
    }
    function handleClick(e) {
      if (props.disabled || !canvasRef.value)
        return;
      updateDotPosition(e.clientX, e.clientY, canvasRef.value.getBoundingClientRect());
    }
    function handleMouseDown(e) {
      e.preventDefault();
      if (props.disabled)
        return;
      isInteracting.value = true;
      window.addEventListener("mousemove", handleMouseMove);
      window.addEventListener("mouseup", handleMouseUp);
      window.addEventListener("touchmove", handleMouseMove);
      window.addEventListener("touchend", handleMouseUp);
    }
    function handleMouseMove(e) {
      if (props.disabled || !canvasRef.value)
        return;
      isInteracting.value = true;
      const coords = getEventCoordinates(e);
      updateDotPosition(coords.clientX, coords.clientY, canvasRef.value.getBoundingClientRect());
    }
    function handleMouseUp() {
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
      window.removeEventListener("touchmove", handleMouseMove);
      window.removeEventListener("touchend", handleMouseUp);
    }
    watch(dotPosition, () => {
      var _a2, _b, _c, _d;
      if (isOutsideUpdate.value) {
        isOutsideUpdate.value = false;
        return;
      }
      if (!canvasRef.value)
        return;
      const {
        x,
        y
      } = dotPosition.value;
      emit("update:color", {
        h: (_b = (_a2 = props.color) == null ? void 0 : _a2.h) != null ? _b : 0,
        s: clamp(x, 0, canvasWidth.value) / canvasWidth.value,
        v: 1 - clamp(y, 0, canvasHeight.value) / canvasHeight.value,
        a: (_d = (_c = props.color) == null ? void 0 : _c.a) != null ? _d : 1
      });
    });
    function updateCanvas() {
      var _a2, _b;
      if (!canvasRef.value)
        return;
      const canvas = canvasRef.value;
      const ctx = canvas.getContext("2d");
      if (!ctx)
        return;
      const saturationGradient = ctx.createLinearGradient(0, 0, canvas.width, 0);
      saturationGradient.addColorStop(0, "hsla(0, 0%, 100%, 1)");
      saturationGradient.addColorStop(1, `hsla(${(_b = (_a2 = props.color) == null ? void 0 : _a2.h) != null ? _b : 0}, 100%, 50%, 1)`);
      ctx.fillStyle = saturationGradient;
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      const valueGradient = ctx.createLinearGradient(0, 0, 0, canvas.height);
      valueGradient.addColorStop(0, "hsla(0, 0%, 100%, 0)");
      valueGradient.addColorStop(1, "hsla(0, 0%, 0%, 1)");
      ctx.fillStyle = valueGradient;
      ctx.fillRect(0, 0, canvas.width, canvas.height);
    }
    watch(() => {
      var _a2;
      return (_a2 = props.color) == null ? void 0 : _a2.h;
    }, updateCanvas, {
      immediate: true
    });
    watch(() => [canvasWidth.value, canvasHeight.value], (newVal, oldVal) => {
      updateCanvas();
      dotPosition.value = {
        x: dotPosition.value.x * newVal[0] / oldVal[0],
        y: dotPosition.value.y * newVal[1] / oldVal[1]
      };
    }, {
      flush: "post"
    });
    watch(() => props.color, () => {
      if (isInteracting.value) {
        isInteracting.value = false;
        return;
      }
      isOutsideUpdate.value = true;
      dotPosition.value = props.color ? {
        x: props.color.s * canvasWidth.value,
        y: (1 - props.color.v) * canvasHeight.value
      } : {
        x: 0,
        y: 0
      };
    }, {
      deep: true,
      immediate: true
    });
    onMounted(() => updateCanvas());
    useRender(() => createVNode("div", {
      "ref": resizeRef,
      "class": "v-color-picker-canvas",
      "onClick": handleClick,
      "onMousedown": handleMouseDown,
      "onTouchstart": handleMouseDown
    }, [createVNode("canvas", {
      "ref": canvasRef,
      "width": canvasWidth.value,
      "height": canvasHeight.value
    }, null), props.color && createVNode("div", {
      "class": ["v-color-picker-canvas__dot", {
        "v-color-picker-canvas__dot--disabled": props.disabled
      }],
      "style": dotStyles.value
    }, null)]));
    return {};
  }
});
var VColorPickerEdit$1 = "";
var VBtn$1 = "";
var VBtnToggle = "";
var VBtnGroup$1 = "";
const makeBorderProps = propsFactory({
  border: [Boolean, Number, String]
}, "border");
function useBorder(props) {
  let name = arguments.length > 1 && arguments[1] !== void 0 ? arguments[1] : getCurrentInstanceName();
  const borderClasses = computed(() => {
    const border = isRef(props) ? props.value : props.border;
    const classes = [];
    if (border === true || border === "") {
      classes.push(`${name}--border`);
    } else if (typeof border === "string" || border === 0) {
      for (const value of String(border).split(" ")) {
        classes.push(`border-${value}`);
      }
    }
    return classes;
  });
  return {
    borderClasses
  };
}
const allowedDensities = [null, "default", "comfortable", "compact"];
const makeDensityProps = propsFactory({
  density: {
    type: String,
    default: "default",
    validator: (v) => allowedDensities.includes(v)
  }
}, "density");
function useDensity(props) {
  let name = arguments.length > 1 && arguments[1] !== void 0 ? arguments[1] : getCurrentInstanceName();
  const densityClasses = computed(() => {
    return `${name}--density-${props.density}`;
  });
  return {
    densityClasses
  };
}
const makeElevationProps = propsFactory({
  elevation: {
    type: [Number, String],
    validator(v) {
      const value = parseInt(v);
      return !isNaN(value) && value >= 0 && value <= 24;
    }
  }
}, "elevation");
function useElevation(props) {
  const elevationClasses = computed(() => {
    const elevation = isRef(props) ? props.value : props.elevation;
    const classes = [];
    if (elevation == null)
      return classes;
    classes.push(`elevation-${elevation}`);
    return classes;
  });
  return {
    elevationClasses
  };
}
const makeRoundedProps = propsFactory({
  rounded: {
    type: [Boolean, Number, String],
    default: void 0
  }
}, "rounded");
function useRounded(props) {
  let name = arguments.length > 1 && arguments[1] !== void 0 ? arguments[1] : getCurrentInstanceName();
  const roundedClasses = computed(() => {
    const rounded = isRef(props) ? props.value : props.rounded;
    const classes = [];
    if (rounded === true || rounded === "") {
      classes.push(`${name}--rounded`);
    } else if (typeof rounded === "string" || rounded === 0) {
      for (const value of String(rounded).split(" ")) {
        classes.push(`rounded-${value}`);
      }
    }
    return classes;
  });
  return {
    roundedClasses
  };
}
function useColor(colors2) {
  return destructComputed(() => {
    const classes = [];
    const styles = {};
    if (colors2.value.background) {
      if (isCssColor(colors2.value.background)) {
        styles.backgroundColor = colors2.value.background;
      } else {
        classes.push(`bg-${colors2.value.background}`);
      }
    }
    if (colors2.value.text) {
      if (isCssColor(colors2.value.text)) {
        styles.color = colors2.value.text;
        styles.caretColor = colors2.value.text;
      } else {
        classes.push(`text-${colors2.value.text}`);
      }
    }
    return {
      colorClasses: classes,
      colorStyles: styles
    };
  });
}
function useTextColor(props, name) {
  const colors2 = computed(() => ({
    text: isRef(props) ? props.value : name ? props[name] : null
  }));
  const {
    colorClasses: textColorClasses,
    colorStyles: textColorStyles
  } = useColor(colors2);
  return {
    textColorClasses,
    textColorStyles
  };
}
function useBackgroundColor(props, name) {
  const colors2 = computed(() => ({
    background: isRef(props) ? props.value : name ? props[name] : null
  }));
  const {
    colorClasses: backgroundColorClasses,
    colorStyles: backgroundColorStyles
  } = useColor(colors2);
  return {
    backgroundColorClasses,
    backgroundColorStyles
  };
}
const allowedVariants = ["elevated", "flat", "tonal", "outlined", "text", "plain"];
function genOverlays(isClickable, name) {
  return createVNode(Fragment, null, [isClickable && createVNode("span", {
    "key": "overlay",
    "class": `${name}__overlay`
  }, null), createVNode("span", {
    "key": "underlay",
    "class": `${name}__underlay`
  }, null)]);
}
const makeVariantProps = propsFactory({
  color: String,
  variant: {
    type: String,
    default: "elevated",
    validator: (v) => allowedVariants.includes(v)
  }
}, "variant");
function useVariant(props) {
  let name = arguments.length > 1 && arguments[1] !== void 0 ? arguments[1] : getCurrentInstanceName();
  const variantClasses = computed(() => {
    const {
      variant
    } = unref(props);
    return `${name}--variant-${variant}`;
  });
  const {
    colorClasses,
    colorStyles
  } = useColor(computed(() => {
    const {
      variant,
      color
    } = unref(props);
    return {
      [["elevated", "flat"].includes(variant) ? "background" : "text"]: color
    };
  }));
  return {
    colorClasses,
    colorStyles,
    variantClasses
  };
}
const makeVBtnGroupProps = propsFactory({
  divided: Boolean,
  ...makeBorderProps(),
  ...makeDensityProps(),
  ...makeElevationProps(),
  ...makeRoundedProps(),
  ...makeTagProps(),
  ...makeThemeProps(),
  ...makeVariantProps()
}, "v-btn-group");
const VBtnGroup = genericComponent()({
  name: "VBtnGroup",
  props: makeVBtnGroupProps(),
  setup(props, _ref) {
    let {
      slots
    } = _ref;
    const {
      themeClasses
    } = provideTheme(props);
    const {
      densityClasses
    } = useDensity(props);
    const {
      borderClasses
    } = useBorder(props);
    const {
      elevationClasses
    } = useElevation(props);
    const {
      roundedClasses
    } = useRounded(props);
    provideDefaults({
      VBtn: {
        height: "auto",
        color: toRef(props, "color"),
        density: toRef(props, "density"),
        flat: true,
        variant: toRef(props, "variant")
      }
    });
    useRender(() => {
      return createVNode(props.tag, {
        "class": ["v-btn-group", {
          "v-btn-group--divided": props.divided
        }, themeClasses.value, borderClasses.value, densityClasses.value, elevationClasses.value, roundedClasses.value]
      }, slots);
    });
  }
});
function filterVBtnGroupProps(props) {
  return pick(props, Object.keys(VBtnGroup.props));
}
const makeGroupProps = propsFactory({
  modelValue: {
    type: null,
    default: void 0
  },
  multiple: Boolean,
  mandatory: [Boolean, String],
  max: Number,
  selectedClass: String,
  disabled: Boolean
}, "group");
const makeGroupItemProps = propsFactory({
  value: null,
  disabled: Boolean,
  selectedClass: String
}, "group-item");
function useGroupItem(props, injectKey) {
  let required = arguments.length > 2 && arguments[2] !== void 0 ? arguments[2] : true;
  const vm = getCurrentInstance("useGroupItem");
  if (!vm) {
    throw new Error("[Vuetify] useGroupItem composable must be used inside a component setup function");
  }
  const id = getUid();
  provide(Symbol.for(`${injectKey.description}:id`), id);
  const group = inject(injectKey, null);
  if (!group) {
    if (!required)
      return group;
    throw new Error(`[Vuetify] Could not find useGroup injection with symbol ${injectKey.description}`);
  }
  const value = toRef(props, "value");
  const disabled = computed(() => group.disabled.value || props.disabled);
  group.register({
    id,
    value,
    disabled
  }, vm);
  onBeforeUnmount(() => {
    group.unregister(id);
  });
  const isSelected = computed(() => {
    return group.isSelected(id);
  });
  const selectedClass = computed(() => isSelected.value && [group.selectedClass.value, props.selectedClass]);
  watch(isSelected, (value2) => {
    vm.emit("group:selected", {
      value: value2
    });
  });
  return {
    id,
    isSelected,
    toggle: () => group.select(id, !isSelected.value),
    select: (value2) => group.select(id, value2),
    selectedClass,
    value,
    disabled,
    group
  };
}
function useGroup(props, injectKey) {
  let isUnmounted = false;
  const items = reactive([]);
  const selected = useProxiedModel(props, "modelValue", [], (v) => {
    if (v == null)
      return [];
    return getIds(items, wrapInArray(v));
  }, (v) => {
    const arr = getValues(items, v);
    return props.multiple ? arr : arr[0];
  });
  const groupVm = getCurrentInstance("useGroup");
  function register(item, vm) {
    const unwrapped = item;
    const key = Symbol.for(`${injectKey.description}:id`);
    const children = findChildrenWithProvide(key, groupVm == null ? void 0 : groupVm.vnode);
    const index = children.indexOf(vm);
    if (index > -1) {
      items.splice(index, 0, unwrapped);
    } else {
      items.push(unwrapped);
    }
  }
  function unregister(id) {
    if (isUnmounted)
      return;
    forceMandatoryValue();
    const index = items.findIndex((item) => item.id === id);
    items.splice(index, 1);
  }
  function forceMandatoryValue() {
    const item = items.find((item2) => !item2.disabled);
    if (item && props.mandatory === "force" && !selected.value.length) {
      selected.value = [item.id];
    }
  }
  onMounted(() => {
    forceMandatoryValue();
  });
  onBeforeUnmount(() => {
    isUnmounted = true;
  });
  function select(id, value) {
    const item = items.find((item2) => item2.id === id);
    if (value && (item == null ? void 0 : item.disabled))
      return;
    if (props.multiple) {
      const internalValue = selected.value.slice();
      const index = internalValue.findIndex((v) => v === id);
      const isSelected = ~index;
      value = value != null ? value : !isSelected;
      if (isSelected && props.mandatory && internalValue.length <= 1)
        return;
      if (!isSelected && props.max != null && internalValue.length + 1 > props.max)
        return;
      if (index < 0 && value)
        internalValue.push(id);
      else if (index >= 0 && !value)
        internalValue.splice(index, 1);
      selected.value = internalValue;
    } else {
      const isSelected = selected.value.includes(id);
      if (props.mandatory && isSelected)
        return;
      selected.value = (value != null ? value : !isSelected) ? [id] : [];
    }
  }
  function step(offset) {
    if (props.multiple)
      consoleWarn('This method is not supported when using "multiple" prop');
    if (!selected.value.length) {
      const item = items.find((item2) => !item2.disabled);
      item && (selected.value = [item.id]);
    } else {
      const currentId = selected.value[0];
      const currentIndex = items.findIndex((i) => i.id === currentId);
      let newIndex = (currentIndex + offset) % items.length;
      let newItem = items[newIndex];
      while (newItem.disabled && newIndex !== currentIndex) {
        newIndex = (newIndex + offset) % items.length;
        newItem = items[newIndex];
      }
      if (newItem.disabled)
        return;
      selected.value = [items[newIndex].id];
    }
  }
  const state = {
    register,
    unregister,
    selected,
    select,
    disabled: toRef(props, "disabled"),
    prev: () => step(items.length - 1),
    next: () => step(1),
    isSelected: (id) => selected.value.includes(id),
    selectedClass: computed(() => props.selectedClass),
    items: computed(() => items),
    getItemIndex: (value) => getItemIndex(items, value)
  };
  provide(injectKey, state);
  return state;
}
function getItemIndex(items, value) {
  const ids = getIds(items, [value]);
  if (!ids.length)
    return -1;
  return items.findIndex((item) => item.id === ids[0]);
}
function getIds(items, modelValue) {
  const ids = [];
  for (let i = 0; i < items.length; i++) {
    const item = items[i];
    if (item.value != null) {
      if (modelValue.find((value) => deepEqual(value, item.value)) != null) {
        ids.push(item.id);
      }
    } else if (modelValue.includes(i)) {
      ids.push(item.id);
    }
  }
  return ids;
}
function getValues(items, ids) {
  const values = [];
  for (let i = 0; i < items.length; i++) {
    const item = items[i];
    if (ids.includes(item.id)) {
      values.push(item.value != null ? item.value : i);
    }
  }
  return values;
}
const VBtnToggleSymbol = Symbol.for("vuetify:v-btn-toggle");
genericComponent()({
  name: "VBtnToggle",
  props: {
    ...makeVBtnGroupProps(),
    ...makeGroupProps()
  },
  emits: {
    "update:modelValue": (value) => true
  },
  setup(props, _ref) {
    let {
      slots
    } = _ref;
    const {
      isSelected,
      next,
      prev,
      select,
      selected
    } = useGroup(props, VBtnToggleSymbol);
    useRender(() => {
      const [btnGroupProps] = filterVBtnGroupProps(props);
      return createVNode(VBtnGroup, mergeProps({
        "class": "v-btn-toggle"
      }, btnGroupProps), {
        default: () => {
          var _a2;
          return [(_a2 = slots.default) == null ? void 0 : _a2.call(slots, {
            isSelected,
            next,
            prev,
            select,
            selected
          })];
        }
      });
    });
    return {
      next,
      prev,
      select
    };
  }
});
const VDefaultsProvider = genericComponent(false)({
  name: "VDefaultsProvider",
  props: {
    defaults: Object,
    reset: [Number, String],
    root: Boolean,
    scoped: Boolean
  },
  setup(props, _ref) {
    let {
      slots
    } = _ref;
    const {
      defaults,
      reset,
      root,
      scoped
    } = toRefs(props);
    provideDefaults(defaults, {
      reset,
      root,
      scoped
    });
    return () => {
      var _a2;
      return (_a2 = slots.default) == null ? void 0 : _a2.call(slots);
    };
  }
});
var VIcon$1 = "";
const predefinedSizes = ["x-small", "small", "default", "large", "x-large"];
const makeSizeProps = propsFactory({
  size: {
    type: [String, Number],
    default: "default"
  }
}, "size");
function useSize(props) {
  let name = arguments.length > 1 && arguments[1] !== void 0 ? arguments[1] : getCurrentInstanceName();
  return destructComputed(() => {
    let sizeClasses;
    let sizeStyles;
    if (includes(predefinedSizes, props.size)) {
      sizeClasses = `${name}--size-${props.size}`;
    } else if (props.size) {
      sizeStyles = {
        width: convertToUnit(props.size),
        height: convertToUnit(props.size)
      };
    }
    return {
      sizeClasses,
      sizeStyles
    };
  });
}
const makeVIconProps = propsFactory({
  color: String,
  start: Boolean,
  end: Boolean,
  icon: IconValue,
  ...makeSizeProps(),
  ...makeTagProps({
    tag: "i"
  }),
  ...makeThemeProps()
}, "v-icon");
const VIcon = genericComponent()({
  name: "VIcon",
  props: makeVIconProps(),
  setup(props, _ref) {
    let {
      attrs,
      slots
    } = _ref;
    let slotIcon;
    if (slots.default) {
      slotIcon = computed(() => {
        var _a2, _b;
        const slot = (_a2 = slots.default) == null ? void 0 : _a2.call(slots);
        if (!slot)
          return;
        return (_b = slot.filter((node) => node.type === Text && node.children && typeof node.children === "string")[0]) == null ? void 0 : _b.children;
      });
    }
    const {
      themeClasses
    } = provideTheme(props);
    const {
      iconData
    } = useIcon(slotIcon || props);
    const {
      sizeClasses
    } = useSize(props);
    const {
      textColorClasses,
      textColorStyles
    } = useTextColor(toRef(props, "color"));
    useRender(() => createVNode(iconData.value.component, {
      "tag": props.tag,
      "icon": iconData.value.icon,
      "class": ["v-icon", "notranslate", themeClasses.value, sizeClasses.value, textColorClasses.value, {
        "v-icon--clickable": !!attrs.onClick,
        "v-icon--start": props.start,
        "v-icon--end": props.end
      }],
      "style": [!sizeClasses.value ? {
        fontSize: convertToUnit(props.size),
        height: convertToUnit(props.size),
        width: convertToUnit(props.size)
      } : void 0, textColorStyles.value],
      "role": attrs.onClick ? "button" : void 0,
      "aria-hidden": !attrs.onClick
    }, {
      default: () => {
        var _a2;
        return [(_a2 = slots.default) == null ? void 0 : _a2.call(slots)];
      }
    }));
    return {};
  }
});
var VProgressCircular$1 = "";
function useIntersectionObserver(callback) {
  const intersectionRef = ref();
  const isIntersecting = ref(false);
  if (SUPPORTS_INTERSECTION) {
    const observer = new IntersectionObserver((entries) => {
      callback == null ? void 0 : callback(entries, observer);
      isIntersecting.value = !!entries.find((entry) => entry.isIntersecting);
    });
    onBeforeUnmount(() => {
      observer.disconnect();
    });
    watch(intersectionRef, (newValue, oldValue) => {
      if (oldValue) {
        observer.unobserve(oldValue);
        isIntersecting.value = false;
      }
      if (newValue)
        observer.observe(newValue);
    }, {
      flush: "post"
    });
  }
  return {
    intersectionRef,
    isIntersecting
  };
}
const VProgressCircular = genericComponent()({
  name: "VProgressCircular",
  props: {
    bgColor: String,
    color: String,
    indeterminate: [Boolean, String],
    modelValue: {
      type: [Number, String],
      default: 0
    },
    rotate: {
      type: [Number, String],
      default: 0
    },
    width: {
      type: [Number, String],
      default: 4
    },
    ...makeSizeProps(),
    ...makeTagProps({
      tag: "div"
    }),
    ...makeThemeProps()
  },
  setup(props, _ref) {
    let {
      slots
    } = _ref;
    const MAGIC_RADIUS_CONSTANT = 20;
    const CIRCUMFERENCE = 2 * Math.PI * MAGIC_RADIUS_CONSTANT;
    const root = ref();
    const {
      themeClasses
    } = provideTheme(props);
    const {
      sizeClasses,
      sizeStyles
    } = useSize(props);
    const {
      textColorClasses,
      textColorStyles
    } = useTextColor(toRef(props, "color"));
    const {
      textColorClasses: underlayColorClasses,
      textColorStyles: underlayColorStyles
    } = useTextColor(toRef(props, "bgColor"));
    const {
      intersectionRef,
      isIntersecting
    } = useIntersectionObserver();
    const {
      resizeRef,
      contentRect
    } = useResizeObserver();
    const normalizedValue = computed(() => Math.max(0, Math.min(100, parseFloat(props.modelValue))));
    const width = computed(() => Number(props.width));
    const size = computed(() => {
      return sizeStyles.value ? Number(props.size) : contentRect.value ? contentRect.value.width : Math.max(width.value, 32);
    });
    const diameter = computed(() => MAGIC_RADIUS_CONSTANT / (1 - width.value / size.value) * 2);
    const strokeWidth = computed(() => width.value / size.value * diameter.value);
    const strokeDashOffset = computed(() => convertToUnit((100 - normalizedValue.value) / 100 * CIRCUMFERENCE));
    watchEffect(() => {
      intersectionRef.value = root.value;
      resizeRef.value = root.value;
    });
    useRender(() => createVNode(props.tag, {
      "ref": root,
      "class": ["v-progress-circular", {
        "v-progress-circular--indeterminate": !!props.indeterminate,
        "v-progress-circular--visible": isIntersecting.value,
        "v-progress-circular--disable-shrink": props.indeterminate === "disable-shrink"
      }, themeClasses.value, sizeClasses.value, textColorClasses.value],
      "style": [sizeStyles.value, textColorStyles.value],
      "role": "progressbar",
      "aria-valuemin": "0",
      "aria-valuemax": "100",
      "aria-valuenow": props.indeterminate ? void 0 : normalizedValue.value
    }, {
      default: () => [createVNode("svg", {
        "style": {
          transform: `rotate(calc(-90deg + ${Number(props.rotate)}deg))`
        },
        "xmlns": "http://www.w3.org/2000/svg",
        "viewBox": `0 0 ${diameter.value} ${diameter.value}`
      }, [createVNode("circle", {
        "class": ["v-progress-circular__underlay", underlayColorClasses.value],
        "style": underlayColorStyles.value,
        "fill": "transparent",
        "cx": "50%",
        "cy": "50%",
        "r": MAGIC_RADIUS_CONSTANT,
        "stroke-width": strokeWidth.value,
        "stroke-dasharray": CIRCUMFERENCE,
        "stroke-dashoffset": 0
      }, null), createVNode("circle", {
        "class": "v-progress-circular__overlay",
        "fill": "transparent",
        "cx": "50%",
        "cy": "50%",
        "r": MAGIC_RADIUS_CONSTANT,
        "stroke-width": strokeWidth.value,
        "stroke-dasharray": CIRCUMFERENCE,
        "stroke-dashoffset": strokeDashOffset.value
      }, null)]), slots.default && createVNode("div", {
        "class": "v-progress-circular__content"
      }, [slots.default({
        value: normalizedValue.value
      })])]
    }));
    return {};
  }
});
var VRipple = "";
const stopSymbol = Symbol("rippleStop");
const DELAY_RIPPLE = 80;
function transform(el, value) {
  el.style.transform = value;
  el.style.webkitTransform = value;
}
function isTouchEvent(e) {
  return e.constructor.name === "TouchEvent";
}
function isKeyboardEvent(e) {
  return e.constructor.name === "KeyboardEvent";
}
const calculate = function(e, el) {
  var _a2;
  let value = arguments.length > 2 && arguments[2] !== void 0 ? arguments[2] : {};
  let localX = 0;
  let localY = 0;
  if (!isKeyboardEvent(e)) {
    const offset = el.getBoundingClientRect();
    const target = isTouchEvent(e) ? e.touches[e.touches.length - 1] : e;
    localX = target.clientX - offset.left;
    localY = target.clientY - offset.top;
  }
  let radius = 0;
  let scale = 0.3;
  if ((_a2 = el._ripple) == null ? void 0 : _a2.circle) {
    scale = 0.15;
    radius = el.clientWidth / 2;
    radius = value.center ? radius : radius + Math.sqrt((localX - radius) ** 2 + (localY - radius) ** 2) / 4;
  } else {
    radius = Math.sqrt(el.clientWidth ** 2 + el.clientHeight ** 2) / 2;
  }
  const centerX = `${(el.clientWidth - radius * 2) / 2}px`;
  const centerY = `${(el.clientHeight - radius * 2) / 2}px`;
  const x = value.center ? centerX : `${localX - radius}px`;
  const y = value.center ? centerY : `${localY - radius}px`;
  return {
    radius,
    scale,
    x,
    y,
    centerX,
    centerY
  };
};
const ripples = {
  show(e, el) {
    var _a2;
    let value = arguments.length > 2 && arguments[2] !== void 0 ? arguments[2] : {};
    if (!((_a2 = el == null ? void 0 : el._ripple) == null ? void 0 : _a2.enabled)) {
      return;
    }
    const container = document.createElement("span");
    const animation = document.createElement("span");
    container.appendChild(animation);
    container.className = "v-ripple__container";
    if (value.class) {
      container.className += ` ${value.class}`;
    }
    const {
      radius,
      scale,
      x,
      y,
      centerX,
      centerY
    } = calculate(e, el, value);
    const size = `${radius * 2}px`;
    animation.className = "v-ripple__animation";
    animation.style.width = size;
    animation.style.height = size;
    el.appendChild(container);
    const computed2 = window.getComputedStyle(el);
    if (computed2 && computed2.position === "static") {
      el.style.position = "relative";
      el.dataset.previousPosition = "static";
    }
    animation.classList.add("v-ripple__animation--enter");
    animation.classList.add("v-ripple__animation--visible");
    transform(animation, `translate(${x}, ${y}) scale3d(${scale},${scale},${scale})`);
    animation.dataset.activated = String(performance.now());
    setTimeout(() => {
      animation.classList.remove("v-ripple__animation--enter");
      animation.classList.add("v-ripple__animation--in");
      transform(animation, `translate(${centerX}, ${centerY}) scale3d(1,1,1)`);
    }, 0);
  },
  hide(el) {
    var _a2;
    if (!((_a2 = el == null ? void 0 : el._ripple) == null ? void 0 : _a2.enabled))
      return;
    const ripples2 = el.getElementsByClassName("v-ripple__animation");
    if (ripples2.length === 0)
      return;
    const animation = ripples2[ripples2.length - 1];
    if (animation.dataset.isHiding)
      return;
    else
      animation.dataset.isHiding = "true";
    const diff = performance.now() - Number(animation.dataset.activated);
    const delay = Math.max(250 - diff, 0);
    setTimeout(() => {
      animation.classList.remove("v-ripple__animation--in");
      animation.classList.add("v-ripple__animation--out");
      setTimeout(() => {
        var _a3;
        const ripples3 = el.getElementsByClassName("v-ripple__animation");
        if (ripples3.length === 1 && el.dataset.previousPosition) {
          el.style.position = el.dataset.previousPosition;
          delete el.dataset.previousPosition;
        }
        if (((_a3 = animation.parentNode) == null ? void 0 : _a3.parentNode) === el)
          el.removeChild(animation.parentNode);
      }, 300);
    }, delay);
  }
};
function isRippleEnabled(value) {
  return typeof value === "undefined" || !!value;
}
function rippleShow(e) {
  const value = {};
  const element = e.currentTarget;
  if (!(element == null ? void 0 : element._ripple) || element._ripple.touched || e[stopSymbol])
    return;
  e[stopSymbol] = true;
  if (isTouchEvent(e)) {
    element._ripple.touched = true;
    element._ripple.isTouch = true;
  } else {
    if (element._ripple.isTouch)
      return;
  }
  value.center = element._ripple.centered || isKeyboardEvent(e);
  if (element._ripple.class) {
    value.class = element._ripple.class;
  }
  if (isTouchEvent(e)) {
    if (element._ripple.showTimerCommit)
      return;
    element._ripple.showTimerCommit = () => {
      ripples.show(e, element, value);
    };
    element._ripple.showTimer = window.setTimeout(() => {
      var _a2;
      if ((_a2 = element == null ? void 0 : element._ripple) == null ? void 0 : _a2.showTimerCommit) {
        element._ripple.showTimerCommit();
        element._ripple.showTimerCommit = null;
      }
    }, DELAY_RIPPLE);
  } else {
    ripples.show(e, element, value);
  }
}
function rippleStop(e) {
  e[stopSymbol] = true;
}
function rippleHide(e) {
  const element = e.currentTarget;
  if (!(element == null ? void 0 : element._ripple))
    return;
  window.clearTimeout(element._ripple.showTimer);
  if (e.type === "touchend" && element._ripple.showTimerCommit) {
    element._ripple.showTimerCommit();
    element._ripple.showTimerCommit = null;
    element._ripple.showTimer = window.setTimeout(() => {
      rippleHide(e);
    });
    return;
  }
  window.setTimeout(() => {
    if (element._ripple) {
      element._ripple.touched = false;
    }
  });
  ripples.hide(element);
}
function rippleCancelShow(e) {
  const element = e.currentTarget;
  if (!(element == null ? void 0 : element._ripple))
    return;
  if (element._ripple.showTimerCommit) {
    element._ripple.showTimerCommit = null;
  }
  window.clearTimeout(element._ripple.showTimer);
}
let keyboardRipple = false;
function keyboardRippleShow(e) {
  if (!keyboardRipple && (e.keyCode === keyCodes.enter || e.keyCode === keyCodes.space)) {
    keyboardRipple = true;
    rippleShow(e);
  }
}
function keyboardRippleHide(e) {
  keyboardRipple = false;
  rippleHide(e);
}
function focusRippleHide(e) {
  if (keyboardRipple) {
    keyboardRipple = false;
    rippleHide(e);
  }
}
function updateRipple(el, binding, wasEnabled) {
  var _a2;
  const {
    value,
    modifiers
  } = binding;
  const enabled = isRippleEnabled(value);
  if (!enabled) {
    ripples.hide(el);
  }
  el._ripple = (_a2 = el._ripple) != null ? _a2 : {};
  el._ripple.enabled = enabled;
  el._ripple.centered = modifiers.center;
  el._ripple.circle = modifiers.circle;
  if (isObject(value) && value.class) {
    el._ripple.class = value.class;
  }
  if (enabled && !wasEnabled) {
    if (modifiers.stop) {
      el.addEventListener("touchstart", rippleStop, {
        passive: true
      });
      el.addEventListener("mousedown", rippleStop);
      return;
    }
    el.addEventListener("touchstart", rippleShow, {
      passive: true
    });
    el.addEventListener("touchend", rippleHide, {
      passive: true
    });
    el.addEventListener("touchmove", rippleCancelShow, {
      passive: true
    });
    el.addEventListener("touchcancel", rippleHide);
    el.addEventListener("mousedown", rippleShow);
    el.addEventListener("mouseup", rippleHide);
    el.addEventListener("mouseleave", rippleHide);
    el.addEventListener("keydown", keyboardRippleShow);
    el.addEventListener("keyup", keyboardRippleHide);
    el.addEventListener("blur", focusRippleHide);
    el.addEventListener("dragstart", rippleHide, {
      passive: true
    });
  } else if (!enabled && wasEnabled) {
    removeListeners(el);
  }
}
function removeListeners(el) {
  el.removeEventListener("mousedown", rippleShow);
  el.removeEventListener("touchstart", rippleShow);
  el.removeEventListener("touchend", rippleHide);
  el.removeEventListener("touchmove", rippleCancelShow);
  el.removeEventListener("touchcancel", rippleHide);
  el.removeEventListener("mouseup", rippleHide);
  el.removeEventListener("mouseleave", rippleHide);
  el.removeEventListener("keydown", keyboardRippleShow);
  el.removeEventListener("keyup", keyboardRippleHide);
  el.removeEventListener("dragstart", rippleHide);
  el.removeEventListener("blur", focusRippleHide);
}
function mounted(el, binding) {
  updateRipple(el, binding, false);
}
function unmounted(el) {
  delete el._ripple;
  removeListeners(el);
}
function updated(el, binding) {
  if (binding.value === binding.oldValue) {
    return;
  }
  const wasEnabled = isRippleEnabled(binding.oldValue);
  updateRipple(el, binding, wasEnabled);
}
const Ripple = {
  mounted,
  unmounted,
  updated
};
const makeDimensionProps = propsFactory({
  height: [Number, String],
  maxHeight: [Number, String],
  maxWidth: [Number, String],
  minHeight: [Number, String],
  minWidth: [Number, String],
  width: [Number, String]
}, "dimension");
function useDimension(props) {
  const dimensionStyles = computed(() => ({
    height: convertToUnit(props.height),
    maxHeight: convertToUnit(props.maxHeight),
    maxWidth: convertToUnit(props.maxWidth),
    minHeight: convertToUnit(props.minHeight),
    minWidth: convertToUnit(props.minWidth),
    width: convertToUnit(props.width)
  }));
  return {
    dimensionStyles
  };
}
const oppositeMap = {
  center: "center",
  top: "bottom",
  bottom: "top",
  left: "right",
  right: "left"
};
const makeLocationProps = propsFactory({
  location: String
}, "location");
function useLocation(props) {
  let opposite = arguments.length > 1 && arguments[1] !== void 0 ? arguments[1] : false;
  let offset = arguments.length > 2 ? arguments[2] : void 0;
  const {
    isRtl
  } = useRtl();
  const locationStyles = computed(() => {
    if (!props.location)
      return {};
    const {
      side,
      align
    } = parseAnchor(props.location.split(" ").length > 1 ? props.location : `${props.location} center`, isRtl.value);
    function getOffset2(side2) {
      return offset ? offset(side2) : 0;
    }
    const styles = {};
    if (side !== "center") {
      if (opposite)
        styles[oppositeMap[side]] = `calc(100% - ${getOffset2(side)}px)`;
      else
        styles[side] = 0;
    }
    if (align !== "center") {
      if (opposite)
        styles[oppositeMap[align]] = `calc(100% - ${getOffset2(align)}px)`;
      else
        styles[align] = 0;
    } else {
      if (side === "center")
        styles.top = styles.left = "50%";
      else {
        styles[{
          top: "left",
          bottom: "left",
          left: "top",
          right: "top"
        }[side]] = "50%";
      }
      styles.transform = {
        top: "translateX(-50%)",
        bottom: "translateX(-50%)",
        left: "translateY(-50%)",
        right: "translateY(-50%)",
        center: "translate(-50%, -50%)"
      }[side];
    }
    return styles;
  });
  return {
    locationStyles
  };
}
const makeLoaderProps = propsFactory({
  loading: [Boolean, String]
}, "loader");
function useLoader(props) {
  let name = arguments.length > 1 && arguments[1] !== void 0 ? arguments[1] : getCurrentInstanceName();
  const loaderClasses = computed(() => ({
    [`${name}--loading`]: props.loading
  }));
  return {
    loaderClasses
  };
}
const positionValues = ["static", "relative", "fixed", "absolute", "sticky"];
const makePositionProps = propsFactory({
  position: {
    type: String,
    validator: (v) => positionValues.includes(v)
  }
}, "position");
function usePosition(props) {
  let name = arguments.length > 1 && arguments[1] !== void 0 ? arguments[1] : getCurrentInstanceName();
  const positionClasses = computed(() => {
    return props.position ? `${name}--${props.position}` : void 0;
  });
  return {
    positionClasses
  };
}
function useLink(props, attrs) {
  const RouterLink = resolveDynamicComponent("RouterLink");
  const isLink = computed(() => !!(props.href || props.to));
  const isClickable = computed(() => {
    return (isLink == null ? void 0 : isLink.value) || hasEvent(attrs, "click") || hasEvent(props, "click");
  });
  if (typeof RouterLink === "string") {
    return {
      isLink,
      isClickable,
      href: toRef(props, "href")
    };
  }
  const link = props.to ? RouterLink.useLink(props) : void 0;
  return {
    isLink,
    isClickable,
    route: link == null ? void 0 : link.route,
    navigate: link == null ? void 0 : link.navigate,
    isActive: link && computed(() => {
      var _a2, _b;
      return props.exact ? (_a2 = link.isExactActive) == null ? void 0 : _a2.value : (_b = link.isActive) == null ? void 0 : _b.value;
    }),
    href: computed(() => props.to ? link == null ? void 0 : link.route.value.href : props.href)
  };
}
const makeRouterProps = propsFactory({
  href: String,
  replace: Boolean,
  to: [String, Object],
  exact: Boolean
}, "router");
function useSelectLink(link, select) {
  watch(() => {
    var _a2;
    return (_a2 = link.isActive) == null ? void 0 : _a2.value;
  }, (isActive) => {
    if (link.isLink.value && isActive && select) {
      nextTick(() => {
        select(true);
      });
    }
  }, {
    immediate: true
  });
}
const VBtn = genericComponent()({
  name: "VBtn",
  directives: {
    Ripple
  },
  props: {
    active: {
      type: Boolean,
      default: void 0
    },
    symbol: {
      type: null,
      default: VBtnToggleSymbol
    },
    flat: Boolean,
    icon: [Boolean, String, Function, Object],
    prependIcon: IconValue,
    appendIcon: IconValue,
    block: Boolean,
    stacked: Boolean,
    ripple: {
      type: Boolean,
      default: true
    },
    ...makeBorderProps(),
    ...makeRoundedProps(),
    ...makeDensityProps(),
    ...makeDimensionProps(),
    ...makeElevationProps(),
    ...makeGroupItemProps(),
    ...makeLoaderProps(),
    ...makeLocationProps(),
    ...makePositionProps(),
    ...makeRouterProps(),
    ...makeSizeProps(),
    ...makeTagProps({
      tag: "button"
    }),
    ...makeThemeProps(),
    ...makeVariantProps({
      variant: "elevated"
    })
  },
  emits: {
    "group:selected": (val) => true
  },
  setup(props, _ref) {
    let {
      attrs,
      slots
    } = _ref;
    const {
      themeClasses
    } = provideTheme(props);
    const {
      borderClasses
    } = useBorder(props);
    const {
      colorClasses,
      colorStyles,
      variantClasses
    } = useVariant(props);
    const {
      densityClasses
    } = useDensity(props);
    const {
      dimensionStyles
    } = useDimension(props);
    const {
      elevationClasses
    } = useElevation(props);
    const {
      loaderClasses
    } = useLoader(props);
    const {
      locationStyles
    } = useLocation(props);
    const {
      positionClasses
    } = usePosition(props);
    const {
      roundedClasses
    } = useRounded(props);
    const {
      sizeClasses,
      sizeStyles
    } = useSize(props);
    const group = useGroupItem(props, props.symbol, false);
    const link = useLink(props, attrs);
    const isActive = computed(() => {
      var _a2;
      if (props.active !== void 0) {
        return props.active;
      }
      if (link.isLink.value) {
        return (_a2 = link.isActive) == null ? void 0 : _a2.value;
      }
      return group == null ? void 0 : group.isSelected.value;
    });
    const isDisabled = computed(() => (group == null ? void 0 : group.disabled.value) || props.disabled);
    const isElevated = computed(() => {
      return props.variant === "elevated" && !(props.disabled || props.flat || props.border);
    });
    const valueAttr = computed(() => {
      if (props.value === void 0)
        return void 0;
      return Object(props.value) === props.value ? JSON.stringify(props.value, null, 0) : props.value;
    });
    useSelectLink(link, group == null ? void 0 : group.select);
    useRender(() => {
      var _a2, _b;
      const Tag = link.isLink.value ? "a" : props.tag;
      const hasPrepend = !!(props.prependIcon || slots.prepend);
      const hasAppend = !!(props.appendIcon || slots.append);
      const hasIcon = !!(props.icon && props.icon !== true);
      const hasColor = (group == null ? void 0 : group.isSelected.value) && (!link.isLink.value || ((_a2 = link.isActive) == null ? void 0 : _a2.value)) || !group || ((_b = link.isActive) == null ? void 0 : _b.value);
      return withDirectives(createVNode(Tag, {
        "type": Tag === "a" ? void 0 : "button",
        "class": ["v-btn", group == null ? void 0 : group.selectedClass.value, {
          "v-btn--active": isActive.value,
          "v-btn--block": props.block,
          "v-btn--disabled": isDisabled.value,
          "v-btn--elevated": isElevated.value,
          "v-btn--flat": props.flat,
          "v-btn--icon": !!props.icon,
          "v-btn--loading": props.loading,
          "v-btn--stacked": props.stacked
        }, themeClasses.value, borderClasses.value, hasColor ? colorClasses.value : void 0, densityClasses.value, elevationClasses.value, loaderClasses.value, positionClasses.value, roundedClasses.value, sizeClasses.value, variantClasses.value],
        "style": [hasColor ? colorStyles.value : void 0, dimensionStyles.value, locationStyles.value, sizeStyles.value],
        "disabled": isDisabled.value || void 0,
        "href": link.href.value,
        "onClick": (e) => {
          var _a3;
          if (isDisabled.value)
            return;
          (_a3 = link.navigate) == null ? void 0 : _a3.call(link, e);
          group == null ? void 0 : group.toggle();
        },
        "value": valueAttr.value
      }, {
        default: () => {
          var _a3, _b2;
          return [genOverlays(true, "v-btn"), !props.icon && hasPrepend && createVNode(VDefaultsProvider, {
            "key": "prepend",
            "defaults": {
              VIcon: {
                icon: props.prependIcon
              }
            }
          }, {
            default: () => {
              var _a4, _b3;
              return [createVNode("span", {
                "class": "v-btn__prepend"
              }, [(_b3 = (_a4 = slots.prepend) == null ? void 0 : _a4.call(slots)) != null ? _b3 : createVNode(VIcon, null, null)])];
            }
          }), createVNode("span", {
            "class": "v-btn__content",
            "data-no-activator": ""
          }, [createVNode(VDefaultsProvider, {
            "key": "content",
            "defaults": {
              VIcon: {
                icon: hasIcon ? props.icon : void 0
              }
            }
          }, {
            default: () => {
              var _a4, _b3;
              return [(_b3 = (_a4 = slots.default) == null ? void 0 : _a4.call(slots)) != null ? _b3 : hasIcon && createVNode(VIcon, {
                "key": "icon"
              }, null)];
            }
          })]), !props.icon && hasAppend && createVNode(VDefaultsProvider, {
            "key": "append",
            "defaults": {
              VIcon: {
                icon: props.appendIcon
              }
            }
          }, {
            default: () => {
              var _a4, _b3;
              return [createVNode("span", {
                "class": "v-btn__append"
              }, [(_b3 = (_a4 = slots.append) == null ? void 0 : _a4.call(slots)) != null ? _b3 : createVNode(VIcon, null, null)])];
            }
          }), !!props.loading && createVNode("span", {
            "key": "loader",
            "class": "v-btn__loader"
          }, [(_b2 = (_a3 = slots.loader) == null ? void 0 : _a3.call(slots)) != null ? _b2 : createVNode(VProgressCircular, {
            "color": typeof props.loading === "boolean" ? void 0 : props.loading,
            "indeterminate": true,
            "size": "23",
            "width": "2"
          }, null)])];
        }
      }), [[resolveDirective("ripple"), !isDisabled.value && props.ripple, null]]);
    });
    return {};
  }
});
function has(obj, key) {
  return key.every((k) => obj.hasOwnProperty(k));
}
function parseColor(color) {
  if (!color)
    return null;
  let hsva = null;
  if (typeof color === "string") {
    const hex2 = parseHex(color);
    hsva = HexToHSV(hex2);
  }
  if (typeof color === "object") {
    if (has(color, ["r", "g", "b"])) {
      hsva = RGBtoHSV(color);
    } else if (has(color, ["h", "s", "l"])) {
      hsva = HSLtoHSV(color);
    } else if (has(color, ["h", "s", "v"])) {
      hsva = color;
    }
  }
  return hsva;
}
function stripAlpha(color, stripAlpha2) {
  if (stripAlpha2) {
    const {
      a,
      ...rest
    } = color;
    return rest;
  }
  return color;
}
function extractColor(color, input) {
  if (input == null || typeof input === "string") {
    const hex2 = HSVtoHex(color);
    if (color.a === 1)
      return hex2.slice(0, 7);
    else
      return hex2;
  }
  if (typeof input === "object") {
    let converted;
    if (has(input, ["r", "g", "b"]))
      converted = HSVtoRGB(color);
    else if (has(input, ["h", "s", "l"]))
      converted = HSVtoHSL(color);
    else if (has(input, ["h", "s", "v"]))
      converted = color;
    return stripAlpha(converted, !has(input, ["a"]) && color.a === 1);
  }
  return color;
}
const nullColor = {
  h: 0,
  s: 0,
  v: 1,
  a: 1
};
const rgba = {
  inputProps: {
    type: "number",
    min: 0
  },
  inputs: [{
    label: "R",
    max: 255,
    step: 1,
    getValue: (c) => Math.round(c.r),
    getColor: (c, v) => ({
      ...c,
      r: Number(v)
    })
  }, {
    label: "G",
    max: 255,
    step: 1,
    getValue: (c) => Math.round(c.g),
    getColor: (c, v) => ({
      ...c,
      g: Number(v)
    })
  }, {
    label: "B",
    max: 255,
    step: 1,
    getValue: (c) => Math.round(c.b),
    getColor: (c, v) => ({
      ...c,
      b: Number(v)
    })
  }, {
    label: "A",
    max: 1,
    step: 0.01,
    getValue: (_ref) => {
      let {
        a
      } = _ref;
      return a ? Math.round(a * 100) / 100 : 1;
    },
    getColor: (c, v) => ({
      ...c,
      a: Number(v)
    })
  }],
  to: HSVtoRGB,
  from: RGBtoHSV
};
const rgb = {
  ...rgba,
  inputs: (_a = rgba.inputs) == null ? void 0 : _a.slice(0, 3)
};
const hsla = {
  inputProps: {
    type: "number",
    min: 0
  },
  inputs: [{
    label: "H",
    max: 360,
    step: 1,
    getValue: (c) => Math.round(c.h),
    getColor: (c, v) => ({
      ...c,
      h: Number(v)
    })
  }, {
    label: "S",
    max: 1,
    step: 0.01,
    getValue: (c) => Math.round(c.s * 100) / 100,
    getColor: (c, v) => ({
      ...c,
      s: Number(v)
    })
  }, {
    label: "L",
    max: 1,
    step: 0.01,
    getValue: (c) => Math.round(c.l * 100) / 100,
    getColor: (c, v) => ({
      ...c,
      l: Number(v)
    })
  }, {
    label: "A",
    max: 1,
    step: 0.01,
    getValue: (_ref2) => {
      let {
        a
      } = _ref2;
      return a ? Math.round(a * 100) / 100 : 1;
    },
    getColor: (c, v) => ({
      ...c,
      a: Number(v)
    })
  }],
  to: HSVtoHSL,
  from: HSLtoHSV
};
const hsl = {
  ...hsla,
  inputs: hsla.inputs.slice(0, 3)
};
const hexa = {
  inputProps: {
    type: "text"
  },
  inputs: [{
    label: "HEXA",
    getValue: (c) => c,
    getColor: (c, v) => v
  }],
  to: HSVtoHex,
  from: HexToHSV
};
const hex = {
  ...hexa,
  inputs: [{
    label: "HEX",
    getValue: (c) => c.slice(0, 7),
    getColor: (c, v) => v
  }]
};
const modes = {
  rgb,
  rgba,
  hsl,
  hsla,
  hex,
  hexa
};
const VColorPickerInput = (_ref) => {
  let {
    label,
    ...rest
  } = _ref;
  return createVNode("div", {
    "class": "v-color-picker-edit__input"
  }, [createVNode("input", rest, null), createVNode("span", null, [label])]);
};
const VColorPickerEdit = defineComponent({
  name: "VColorPickerEdit",
  props: {
    color: Object,
    disabled: Boolean,
    mode: {
      type: String,
      default: "rgba",
      validator: (v) => Object.keys(modes).includes(v)
    },
    modes: {
      type: Array,
      default: () => Object.keys(modes),
      validator: (v) => Array.isArray(v) && v.every((m) => Object.keys(modes).includes(m))
    }
  },
  emits: {
    "update:color": (color) => true,
    "update:mode": (mode) => true
  },
  setup(props, _ref2) {
    let {
      emit
    } = _ref2;
    const enabledModes = computed(() => {
      return props.modes.map((key) => ({
        ...modes[key],
        name: key
      }));
    });
    const inputs = computed(() => {
      var _a2;
      const mode = enabledModes.value.find((m) => m.name === props.mode);
      if (!mode)
        return [];
      const color = props.color ? mode.to(props.color) : null;
      return (_a2 = mode.inputs) == null ? void 0 : _a2.map((_ref3) => {
        let {
          getValue,
          getColor,
          ...inputProps
        } = _ref3;
        return {
          ...mode.inputProps,
          ...inputProps,
          disabled: props.disabled,
          value: color && getValue(color),
          onChange: (e) => {
            const target = e.target;
            if (!target)
              return;
            emit("update:color", mode.from(getColor(color != null ? color : nullColor, target.value)));
          }
        };
      });
    });
    useRender(() => {
      var _a2;
      return createVNode("div", {
        "class": "v-color-picker-edit"
      }, [(_a2 = inputs.value) == null ? void 0 : _a2.map((props2) => createVNode(VColorPickerInput, props2, null)), enabledModes.value.length > 1 && createVNode(VBtn, {
        "icon": "$unfold",
        "size": "x-small",
        "variant": "plain",
        "onClick": () => {
          const mi = enabledModes.value.findIndex((m) => m.name === props.mode);
          emit("update:mode", enabledModes.value[(mi + 1) % enabledModes.value.length].name);
        }
      }, null)]);
    });
    return {};
  }
});
var VColorPickerPreview$1 = "";
var VSlider$1 = "";
var VInput$1 = "";
var VMessages$1 = "";
function createCssTransition(name) {
  let origin = arguments.length > 1 && arguments[1] !== void 0 ? arguments[1] : "center center";
  let mode = arguments.length > 2 ? arguments[2] : void 0;
  return genericComponent()({
    name,
    props: {
      group: Boolean,
      hideOnLeave: Boolean,
      leaveAbsolute: Boolean,
      mode: {
        type: String,
        default: mode
      },
      origin: {
        type: String,
        default: origin
      }
    },
    setup(props, _ref) {
      let {
        slots
      } = _ref;
      return () => {
        const tag = props.group ? TransitionGroup : Transition;
        return h(tag, {
          name,
          mode: props.mode,
          onBeforeEnter(el) {
            el.style.transformOrigin = props.origin;
          },
          onLeave(el) {
            if (props.leaveAbsolute) {
              const {
                offsetTop,
                offsetLeft,
                offsetWidth,
                offsetHeight
              } = el;
              el._transitionInitialStyles = {
                position: el.style.position,
                top: el.style.top,
                left: el.style.left,
                width: el.style.width,
                height: el.style.height
              };
              el.style.position = "absolute";
              el.style.top = `${offsetTop}px`;
              el.style.left = `${offsetLeft}px`;
              el.style.width = `${offsetWidth}px`;
              el.style.height = `${offsetHeight}px`;
            }
            if (props.hideOnLeave) {
              el.style.setProperty("display", "none", "important");
            }
          },
          onAfterLeave(el) {
            if (props.leaveAbsolute && (el == null ? void 0 : el._transitionInitialStyles)) {
              const {
                position,
                top,
                left,
                width,
                height
              } = el._transitionInitialStyles;
              delete el._transitionInitialStyles;
              el.style.position = position || "";
              el.style.top = top || "";
              el.style.left = left || "";
              el.style.width = width || "";
              el.style.height = height || "";
            }
          }
        }, slots.default);
      };
    }
  });
}
function createJavascriptTransition(name, functions) {
  let mode = arguments.length > 2 && arguments[2] !== void 0 ? arguments[2] : "in-out";
  return genericComponent()({
    name,
    props: {
      mode: {
        type: String,
        default: mode
      }
    },
    setup(props, _ref2) {
      let {
        slots
      } = _ref2;
      return () => {
        return h(Transition, {
          name,
          ...functions
        }, slots.default);
      };
    }
  });
}
function ExpandTransitionGenerator() {
  let expandedParentClass = arguments.length > 0 && arguments[0] !== void 0 ? arguments[0] : "";
  let x = arguments.length > 1 && arguments[1] !== void 0 ? arguments[1] : false;
  const sizeProperty = x ? "width" : "height";
  const offsetProperty = camelize(`offset-${sizeProperty}`);
  return {
    onBeforeEnter(el) {
      el._parent = el.parentNode;
      el._initialStyle = {
        transition: el.style.transition,
        overflow: el.style.overflow,
        [sizeProperty]: el.style[sizeProperty]
      };
    },
    onEnter(el) {
      const initialStyle = el._initialStyle;
      el.style.setProperty("transition", "none", "important");
      el.style.overflow = "hidden";
      const offset = `${el[offsetProperty]}px`;
      el.style[sizeProperty] = "0";
      void el.offsetHeight;
      el.style.transition = initialStyle.transition;
      if (expandedParentClass && el._parent) {
        el._parent.classList.add(expandedParentClass);
      }
      requestAnimationFrame(() => {
        el.style[sizeProperty] = offset;
      });
    },
    onAfterEnter: resetStyles,
    onEnterCancelled: resetStyles,
    onLeave(el) {
      el._initialStyle = {
        transition: "",
        overflow: el.style.overflow,
        [sizeProperty]: el.style[sizeProperty]
      };
      el.style.overflow = "hidden";
      el.style[sizeProperty] = `${el[offsetProperty]}px`;
      void el.offsetHeight;
      requestAnimationFrame(() => el.style[sizeProperty] = "0");
    },
    onAfterLeave,
    onLeaveCancelled: onAfterLeave
  };
  function onAfterLeave(el) {
    if (expandedParentClass && el._parent) {
      el._parent.classList.remove(expandedParentClass);
    }
    resetStyles(el);
  }
  function resetStyles(el) {
    const size = el._initialStyle[sizeProperty];
    el.style.overflow = el._initialStyle.overflow;
    if (size != null)
      el.style[sizeProperty] = size;
    delete el._initialStyle;
  }
}
createCssTransition("fab-transition", "center center", "out-in");
createCssTransition("dialog-bottom-transition");
createCssTransition("dialog-top-transition");
createCssTransition("fade-transition");
const VScaleTransition = createCssTransition("scale-transition");
createCssTransition("scroll-x-transition");
createCssTransition("scroll-x-reverse-transition");
createCssTransition("scroll-y-transition");
createCssTransition("scroll-y-reverse-transition");
createCssTransition("slide-x-transition");
createCssTransition("slide-x-reverse-transition");
const VSlideYTransition = createCssTransition("slide-y-transition");
createCssTransition("slide-y-reverse-transition");
createJavascriptTransition("expand-transition", ExpandTransitionGenerator());
createJavascriptTransition("expand-x-transition", ExpandTransitionGenerator("", true));
const makeTransitionProps = propsFactory({
  transition: {
    type: [Boolean, String, Object],
    default: "fade-transition",
    validator: (val) => val !== true
  }
}, "transition");
const MaybeTransition = (props, _ref) => {
  let {
    slots
  } = _ref;
  const {
    transition,
    ...rest
  } = props;
  const {
    component = Transition,
    ...customProps
  } = typeof transition === "object" ? transition : {};
  return h(component, mergeProps(typeof transition === "string" ? {
    name: transition
  } : customProps, rest), slots);
};
const VMessages = genericComponent()({
  name: "VMessages",
  props: {
    active: Boolean,
    color: String,
    messages: {
      type: [Array, String],
      default: () => []
    },
    ...makeTransitionProps({
      transition: {
        component: VSlideYTransition,
        leaveAbsolute: true,
        group: true
      }
    })
  },
  setup(props, _ref) {
    let {
      slots
    } = _ref;
    const messages = computed(() => wrapInArray(props.messages));
    const {
      textColorClasses,
      textColorStyles
    } = useTextColor(computed(() => props.color));
    useRender(() => createVNode(MaybeTransition, {
      "transition": props.transition,
      "tag": "div",
      "class": ["v-messages", textColorClasses.value],
      "style": textColorStyles.value,
      "role": "alert",
      "aria-live": "polite"
    }, {
      default: () => [props.active && messages.value.map((message, i) => createVNode("div", {
        "class": "v-messages__message",
        "key": `${i}-${messages.value}`
      }, [slots.message ? slots.message({
        message
      }) : message]))]
    }));
    return {};
  }
});
const FormKey = Symbol.for("vuetify:form");
function useForm() {
  return inject(FormKey, null);
}
const makeFocusProps = propsFactory({
  focused: Boolean
}, "focus");
function useFocus(props) {
  let name = arguments.length > 1 && arguments[1] !== void 0 ? arguments[1] : getCurrentInstanceName();
  const isFocused = useProxiedModel(props, "focused");
  const focusClasses = computed(() => {
    return {
      [`${name}--focused`]: isFocused.value
    };
  });
  function focus() {
    isFocused.value = true;
  }
  function blur() {
    isFocused.value = false;
  }
  return {
    focusClasses,
    isFocused,
    focus,
    blur
  };
}
const makeValidationProps = propsFactory({
  disabled: Boolean,
  error: Boolean,
  errorMessages: {
    type: [Array, String],
    default: () => []
  },
  maxErrors: {
    type: [Number, String],
    default: 1
  },
  name: String,
  label: String,
  readonly: Boolean,
  rules: {
    type: Array,
    default: () => []
  },
  modelValue: null,
  validateOn: String,
  validationValue: null,
  ...makeFocusProps()
}, "validation");
function useValidation(props) {
  let name = arguments.length > 1 && arguments[1] !== void 0 ? arguments[1] : getCurrentInstanceName();
  let id = arguments.length > 2 && arguments[2] !== void 0 ? arguments[2] : getUid();
  const model = useProxiedModel(props, "modelValue");
  const validationModel = computed(() => props.validationValue === void 0 ? model.value : props.validationValue);
  const form = useForm();
  const internalErrorMessages = ref([]);
  const isPristine = ref(true);
  const isDirty = computed(() => !!(wrapInArray(model.value === "" ? null : model.value).length || wrapInArray(validationModel.value === "" ? null : validationModel.value).length));
  const isDisabled = computed(() => !!(props.disabled || (form == null ? void 0 : form.isDisabled.value)));
  const isReadonly = computed(() => !!(props.readonly || (form == null ? void 0 : form.isReadonly.value)));
  const errorMessages = computed(() => {
    return props.errorMessages.length ? wrapInArray(props.errorMessages).slice(0, Math.max(0, +props.maxErrors)) : internalErrorMessages.value;
  });
  const isValid = computed(() => {
    if (props.error || errorMessages.value.length)
      return false;
    if (!props.rules.length)
      return true;
    return isPristine.value ? null : true;
  });
  const isValidating = ref(false);
  const validationClasses = computed(() => {
    return {
      [`${name}--error`]: isValid.value === false,
      [`${name}--dirty`]: isDirty.value,
      [`${name}--disabled`]: isDisabled.value,
      [`${name}--readonly`]: isReadonly.value
    };
  });
  const uid = computed(() => {
    var _a2;
    return (_a2 = props.name) != null ? _a2 : unref(id);
  });
  onBeforeMount(() => {
    form == null ? void 0 : form.register({
      id: uid.value,
      validate,
      reset,
      resetValidation
    });
  });
  onBeforeUnmount(() => {
    form == null ? void 0 : form.unregister(uid.value);
  });
  const validateOn = computed(() => props.validateOn || (form == null ? void 0 : form.validateOn.value) || "input");
  onMounted(() => form == null ? void 0 : form.update(uid.value, isValid.value, errorMessages.value));
  useToggleScope(() => validateOn.value === "input", () => {
    watch(validationModel, () => {
      if (validationModel.value != null) {
        validate();
      } else if (props.focused) {
        const unwatch = watch(() => props.focused, (val) => {
          if (!val)
            validate();
          unwatch();
        });
      }
    });
  });
  useToggleScope(() => validateOn.value === "blur", () => {
    watch(() => props.focused, (val) => {
      if (!val)
        validate();
    });
  });
  watch(isValid, () => {
    form == null ? void 0 : form.update(uid.value, isValid.value, errorMessages.value);
  });
  function reset() {
    resetValidation();
    model.value = null;
  }
  function resetValidation() {
    isPristine.value = true;
    internalErrorMessages.value = [];
  }
  async function validate() {
    var _a2;
    const results = [];
    isValidating.value = true;
    for (const rule of props.rules) {
      if (results.length >= ((_a2 = props.maxErrors) != null ? _a2 : 1)) {
        break;
      }
      const handler = typeof rule === "function" ? rule : () => rule;
      const result = await handler(validationModel.value);
      if (result === true)
        continue;
      if (typeof result !== "string") {
        console.warn(`${result} is not a valid value. Rule functions must return boolean true or a string.`);
        continue;
      }
      results.push(result);
    }
    internalErrorMessages.value = results;
    isValidating.value = false;
    isPristine.value = false;
    return internalErrorMessages.value;
  }
  return {
    errorMessages,
    isDirty,
    isDisabled,
    isReadonly,
    isPristine,
    isValid,
    isValidating,
    reset,
    resetValidation,
    validate,
    validationClasses
  };
}
function useInputIcon(props) {
  const {
    t
  } = useLocale();
  function InputIcon(_ref) {
    var _a2;
    let {
      name
    } = _ref;
    const localeKey = {
      prepend: "prependAction",
      prependInner: "prependAction",
      append: "appendAction",
      appendInner: "appendAction",
      clear: "clear"
    }[name];
    const listener = props[`onClick:${name}`];
    const label = listener && localeKey ? t(`$vuetify.input.${localeKey}`, (_a2 = props.label) != null ? _a2 : "") : void 0;
    return createVNode(VIcon, {
      "icon": props[`${name}Icon`],
      "aria-label": label,
      "onClick": listener
    }, null);
  }
  return {
    InputIcon
  };
}
const makeVInputProps = propsFactory({
  id: String,
  appendIcon: IconValue,
  prependIcon: IconValue,
  hideDetails: [Boolean, String],
  messages: {
    type: [Array, String],
    default: () => []
  },
  direction: {
    type: String,
    default: "horizontal",
    validator: (v) => ["horizontal", "vertical"].includes(v)
  },
  "onClick:prepend": EventProp,
  "onClick:append": EventProp,
  ...makeDensityProps(),
  ...makeValidationProps()
}, "v-input");
const VInput = genericComponent()({
  name: "VInput",
  props: {
    ...makeVInputProps()
  },
  emits: {
    "update:modelValue": (val) => true
  },
  setup(props, _ref) {
    let {
      attrs,
      slots,
      emit
    } = _ref;
    const {
      densityClasses
    } = useDensity(props);
    const {
      InputIcon
    } = useInputIcon(props);
    const uid = getUid();
    const id = computed(() => props.id || `input-${uid}`);
    const messagesId = computed(() => `${id.value}-messages`);
    const {
      errorMessages,
      isDirty,
      isDisabled,
      isReadonly,
      isPristine,
      isValid,
      isValidating,
      reset,
      resetValidation,
      validate,
      validationClasses
    } = useValidation(props, "v-input", id);
    const slotProps = computed(() => ({
      id,
      messagesId,
      isDirty,
      isDisabled,
      isReadonly,
      isPristine,
      isValid,
      isValidating,
      reset,
      resetValidation,
      validate
    }));
    useRender(() => {
      var _a2, _b, _c, _d, _e;
      const hasPrepend = !!(slots.prepend || props.prependIcon);
      const hasAppend = !!(slots.append || props.appendIcon);
      const hasMessages = !!(((_a2 = props.messages) == null ? void 0 : _a2.length) || errorMessages.value.length);
      const hasDetails = !props.hideDetails || props.hideDetails === "auto" && (hasMessages || !!slots.details);
      return createVNode("div", {
        "class": ["v-input", `v-input--${props.direction}`, densityClasses.value, validationClasses.value]
      }, [hasPrepend && createVNode("div", {
        "key": "prepend",
        "class": "v-input__prepend"
      }, [(_b = slots.prepend) == null ? void 0 : _b.call(slots, slotProps.value), props.prependIcon && createVNode(InputIcon, {
        "key": "prepend-icon",
        "name": "prepend"
      }, null)]), slots.default && createVNode("div", {
        "class": "v-input__control"
      }, [(_c = slots.default) == null ? void 0 : _c.call(slots, slotProps.value)]), hasAppend && createVNode("div", {
        "key": "append",
        "class": "v-input__append"
      }, [props.appendIcon && createVNode(InputIcon, {
        "key": "append-icon",
        "name": "append"
      }, null), (_d = slots.append) == null ? void 0 : _d.call(slots, slotProps.value)]), hasDetails && createVNode("div", {
        "class": "v-input__details"
      }, [createVNode(VMessages, {
        "id": messagesId.value,
        "active": hasMessages,
        "messages": errorMessages.value.length > 0 ? errorMessages.value : props.messages
      }, {
        message: slots.message
      }), (_e = slots.details) == null ? void 0 : _e.call(slots, slotProps.value)])]);
    });
    return {
      reset,
      resetValidation,
      validate
    };
  }
});
function filterInputProps(props) {
  const keys = Object.keys(VInput.props).filter((k) => !isOn(k));
  return pick(props, keys);
}
var VLabel$1 = "";
const VLabel = genericComponent()({
  name: "VLabel",
  props: {
    text: String,
    clickable: Boolean,
    ...makeThemeProps()
  },
  setup(props, _ref) {
    let {
      slots
    } = _ref;
    useRender(() => {
      var _a2;
      return createVNode("label", {
        "class": ["v-label", {
          "v-label--clickable": props.clickable
        }]
      }, [props.text, (_a2 = slots.default) == null ? void 0 : _a2.call(slots)]);
    });
    return {};
  }
});
var VSliderThumb$1 = "";
const VSliderSymbol = Symbol.for("vuetify:v-slider");
function getOffset(e, el, direction) {
  const vertical = direction === "vertical";
  const rect = el.getBoundingClientRect();
  const touch = "touches" in e ? e.touches[0] : e;
  return vertical ? touch.clientY - (rect.top + rect.height / 2) : touch.clientX - (rect.left + rect.width / 2);
}
function getPosition(e, position) {
  if ("touches" in e && e.touches.length)
    return e.touches[0][position];
  else if ("changedTouches" in e && e.changedTouches.length)
    return e.changedTouches[0][position];
  else
    return e[position];
}
const makeSliderProps = propsFactory({
  disabled: Boolean,
  error: Boolean,
  readonly: Boolean,
  max: {
    type: [Number, String],
    default: 100
  },
  min: {
    type: [Number, String],
    default: 0
  },
  step: {
    type: [Number, String],
    default: 0
  },
  thumbColor: String,
  thumbLabel: {
    type: [Boolean, String],
    default: void 0,
    validator: (v) => typeof v === "boolean" || v === "always"
  },
  thumbSize: {
    type: [Number, String],
    default: 20
  },
  showTicks: {
    type: [Boolean, String],
    default: false,
    validator: (v) => typeof v === "boolean" || v === "always"
  },
  ticks: {
    type: [Array, Object]
  },
  tickSize: {
    type: [Number, String],
    default: 2
  },
  color: String,
  trackColor: String,
  trackFillColor: String,
  trackSize: {
    type: [Number, String],
    default: 4
  },
  direction: {
    type: String,
    default: "horizontal",
    validator: (v) => ["vertical", "horizontal"].includes(v)
  },
  reverse: Boolean,
  ...makeRoundedProps(),
  ...makeElevationProps({
    elevation: 2
  })
}, "slider");
const useSlider = (_ref) => {
  let {
    props,
    handleSliderMouseUp,
    handleMouseMove,
    getActiveThumb
  } = _ref;
  const {
    isRtl
  } = useRtl();
  const isReversed = toRef(props, "reverse");
  const horizontalDirection = computed(() => {
    let hd = isRtl.value ? "rtl" : "ltr";
    if (props.reverse) {
      hd = hd === "rtl" ? "ltr" : "rtl";
    }
    return hd;
  });
  const min = computed(() => parseFloat(props.min));
  const max = computed(() => parseFloat(props.max));
  const step = computed(() => props.step > 0 ? parseFloat(props.step) : 0);
  const decimals = computed(() => {
    const trimmedStep = step.value.toString().trim();
    return trimmedStep.includes(".") ? trimmedStep.length - trimmedStep.indexOf(".") - 1 : 0;
  });
  const thumbSize = computed(() => parseInt(props.thumbSize, 10));
  const tickSize = computed(() => parseInt(props.tickSize, 10));
  const trackSize = computed(() => parseInt(props.trackSize, 10));
  const numTicks = computed(() => (max.value - min.value) / step.value);
  const disabled = toRef(props, "disabled");
  const vertical = computed(() => props.direction === "vertical");
  const thumbColor = computed(() => {
    var _a2;
    return props.error || props.disabled ? void 0 : (_a2 = props.thumbColor) != null ? _a2 : props.color;
  });
  const trackColor = computed(() => {
    var _a2;
    return props.error || props.disabled ? void 0 : (_a2 = props.trackColor) != null ? _a2 : props.color;
  });
  const trackFillColor = computed(() => {
    var _a2;
    return props.error || props.disabled ? void 0 : (_a2 = props.trackFillColor) != null ? _a2 : props.color;
  });
  const mousePressed = ref(false);
  const startOffset = ref(0);
  const trackContainerRef = ref();
  const activeThumbRef = ref();
  function roundValue(value) {
    if (step.value <= 0)
      return value;
    const clamped = clamp(value, min.value, max.value);
    const offset = min.value % step.value;
    const newValue = Math.round((clamped - offset) / step.value) * step.value + offset;
    return parseFloat(Math.min(newValue, max.value).toFixed(decimals.value));
  }
  function parseMouseMove(e) {
    var _a2;
    const vertical2 = props.direction === "vertical";
    const start = vertical2 ? "top" : "left";
    const length = vertical2 ? "height" : "width";
    const position2 = vertical2 ? "clientY" : "clientX";
    const {
      [start]: trackStart,
      [length]: trackLength
    } = (_a2 = trackContainerRef.value) == null ? void 0 : _a2.$el.getBoundingClientRect();
    const clickOffset = getPosition(e, position2);
    let clickPos = Math.min(Math.max((clickOffset - trackStart - startOffset.value) / trackLength, 0), 1) || 0;
    if (vertical2 || horizontalDirection.value === "rtl")
      clickPos = 1 - clickPos;
    return roundValue(min.value + clickPos * (max.value - min.value));
  }
  let thumbMoved = false;
  const handleStop = (e) => {
    if (!thumbMoved) {
      startOffset.value = 0;
      handleSliderMouseUp(parseMouseMove(e));
    }
    mousePressed.value = false;
    thumbMoved = false;
    startOffset.value = 0;
  };
  const handleStart = (e) => {
    activeThumbRef.value = getActiveThumb(e);
    if (!activeThumbRef.value)
      return;
    activeThumbRef.value.focus();
    mousePressed.value = true;
    if (activeThumbRef.value.contains(e.target)) {
      thumbMoved = true;
      startOffset.value = getOffset(e, activeThumbRef.value, props.direction);
    } else {
      startOffset.value = 0;
      handleMouseMove(parseMouseMove(e));
    }
  };
  const moveListenerOptions = {
    passive: true,
    capture: true
  };
  function onMouseMove(e) {
    thumbMoved = true;
    handleMouseMove(parseMouseMove(e));
  }
  function onSliderMouseUp(e) {
    e.stopPropagation();
    e.preventDefault();
    handleStop(e);
    window.removeEventListener("mousemove", onMouseMove, moveListenerOptions);
    window.removeEventListener("mouseup", onSliderMouseUp);
  }
  function onSliderTouchend(e) {
    var _a2;
    handleStop(e);
    window.removeEventListener("touchmove", onMouseMove, moveListenerOptions);
    (_a2 = e.target) == null ? void 0 : _a2.removeEventListener("touchend", onSliderTouchend);
  }
  function onSliderTouchstart(e) {
    var _a2;
    handleStart(e);
    window.addEventListener("touchmove", onMouseMove, moveListenerOptions);
    (_a2 = e.target) == null ? void 0 : _a2.addEventListener("touchend", onSliderTouchend, {
      passive: false
    });
  }
  function onSliderMousedown(e) {
    e.preventDefault();
    handleStart(e);
    window.addEventListener("mousemove", onMouseMove, moveListenerOptions);
    window.addEventListener("mouseup", onSliderMouseUp, {
      passive: false
    });
  }
  const position = (val) => {
    const percentage = (val - min.value) / (max.value - min.value) * 100;
    return clamp(isNaN(percentage) ? 0 : percentage, 0, 100);
  };
  const parsedTicks = computed(() => {
    if (!props.ticks) {
      return numTicks.value !== Infinity ? createRange(numTicks.value + 1).map((t) => {
        const value = min.value + t * step.value;
        return {
          value,
          position: position(value)
        };
      }) : [];
    }
    if (Array.isArray(props.ticks))
      return props.ticks.map((t) => ({
        value: t,
        position: position(t),
        label: t.toString()
      }));
    return Object.keys(props.ticks).map((key) => ({
      value: parseFloat(key),
      position: position(parseFloat(key)),
      label: props.ticks[key]
    }));
  });
  const hasLabels = computed(() => parsedTicks.value.some((_ref2) => {
    let {
      label
    } = _ref2;
    return !!label;
  }));
  const data = {
    activeThumbRef,
    color: toRef(props, "color"),
    decimals,
    disabled,
    direction: toRef(props, "direction"),
    elevation: toRef(props, "elevation"),
    hasLabels,
    horizontalDirection,
    isReversed,
    min,
    max,
    mousePressed,
    numTicks,
    onSliderMousedown,
    onSliderTouchstart,
    parsedTicks,
    parseMouseMove,
    position,
    readonly: toRef(props, "readonly"),
    rounded: toRef(props, "rounded"),
    roundValue,
    showTicks: toRef(props, "showTicks"),
    startOffset,
    step,
    thumbSize,
    thumbColor,
    thumbLabel: toRef(props, "thumbLabel"),
    ticks: toRef(props, "ticks"),
    tickSize,
    trackColor,
    trackContainerRef,
    trackFillColor,
    trackSize,
    vertical
  };
  provide(VSliderSymbol, data);
  return data;
};
const VSliderThumb = genericComponent()({
  name: "VSliderThumb",
  directives: {
    Ripple
  },
  props: {
    focused: Boolean,
    max: {
      type: Number,
      required: true
    },
    min: {
      type: Number,
      required: true
    },
    modelValue: {
      type: Number,
      required: true
    },
    position: {
      type: Number,
      required: true
    },
    ripple: {
      type: Boolean,
      default: true
    }
  },
  emits: {
    "update:modelValue": (v) => true
  },
  setup(props, _ref) {
    let {
      slots,
      emit
    } = _ref;
    const slider = inject(VSliderSymbol);
    if (!slider)
      throw new Error("[Vuetify] v-slider-thumb must be used inside v-slider or v-range-slider");
    const {
      thumbColor,
      step,
      vertical,
      disabled,
      thumbSize,
      thumbLabel,
      direction,
      readonly,
      elevation,
      isReversed,
      horizontalDirection,
      mousePressed,
      decimals
    } = slider;
    const {
      textColorClasses,
      textColorStyles
    } = useTextColor(thumbColor);
    const {
      pageup,
      pagedown,
      end,
      home,
      left,
      right,
      down,
      up
    } = keyValues;
    const relevantKeys = [pageup, pagedown, end, home, left, right, down, up];
    const multipliers = computed(() => {
      if (step.value)
        return [1, 2, 3];
      else
        return [1, 5, 10];
    });
    function parseKeydown(e, value) {
      if (!relevantKeys.includes(e.key))
        return;
      e.preventDefault();
      const _step = step.value || 0.1;
      const steps = (props.max - props.min) / _step;
      if ([left, right, down, up].includes(e.key)) {
        const increase = horizontalDirection.value === "rtl" ? [left, up] : [right, up];
        const direction2 = increase.includes(e.key) ? 1 : -1;
        const multiplier = e.shiftKey ? 2 : e.ctrlKey ? 1 : 0;
        value = value + direction2 * _step * multipliers.value[multiplier];
      } else if (e.key === home) {
        value = props.min;
      } else if (e.key === end) {
        value = props.max;
      } else {
        const direction2 = e.key === pagedown ? 1 : -1;
        value = value - direction2 * _step * (steps > 100 ? steps / 10 : 10);
      }
      return Math.max(props.min, Math.min(props.max, value));
    }
    function onKeydown(e) {
      const newValue = parseKeydown(e, props.modelValue);
      newValue != null && emit("update:modelValue", newValue);
    }
    useRender(() => {
      const positionPercentage = convertToUnit(vertical.value || isReversed.value ? 100 - props.position : props.position, "%");
      const {
        elevationClasses
      } = useElevation(computed(() => !disabled.value ? elevation.value : void 0));
      return createVNode("div", {
        "class": ["v-slider-thumb", {
          "v-slider-thumb--focused": props.focused,
          "v-slider-thumb--pressed": props.focused && mousePressed.value
        }],
        "style": {
          "--v-slider-thumb-position": positionPercentage,
          "--v-slider-thumb-size": convertToUnit(thumbSize.value)
        },
        "role": "slider",
        "tabindex": disabled.value ? -1 : 0,
        "aria-valuemin": props.min,
        "aria-valuemax": props.max,
        "aria-valuenow": props.modelValue,
        "aria-readonly": readonly.value,
        "aria-orientation": direction.value,
        "onKeydown": !readonly.value ? onKeydown : void 0
      }, [createVNode("div", {
        "class": ["v-slider-thumb__surface", textColorClasses.value, elevationClasses.value],
        "style": {
          ...textColorStyles.value
        }
      }, null), withDirectives(createVNode("div", {
        "class": ["v-slider-thumb__ripple", textColorClasses.value],
        "style": textColorStyles.value
      }, null), [[resolveDirective("ripple"), props.ripple, null, {
        circle: true,
        center: true
      }]]), createVNode(VScaleTransition, {
        "origin": "bottom center"
      }, {
        default: () => {
          var _a2, _b;
          return [withDirectives(createVNode("div", {
            "class": "v-slider-thumb__label-container"
          }, [createVNode("div", {
            "class": ["v-slider-thumb__label"]
          }, [createVNode("div", null, [(_b = (_a2 = slots["thumb-label"]) == null ? void 0 : _a2.call(slots, {
            modelValue: props.modelValue
          })) != null ? _b : props.modelValue.toFixed(step.value ? decimals.value : 1)])])]), [[vShow, thumbLabel.value && props.focused || thumbLabel.value === "always"]])];
        }
      })]);
    });
    return {};
  }
});
var VSliderTrack$1 = "";
const VSliderTrack = genericComponent()({
  name: "VSliderTrack",
  props: {
    start: {
      type: Number,
      required: true
    },
    stop: {
      type: Number,
      required: true
    }
  },
  emits: {},
  setup(props, _ref) {
    let {
      slots
    } = _ref;
    const slider = inject(VSliderSymbol);
    if (!slider)
      throw new Error("[Vuetify] v-slider-track must be inside v-slider or v-range-slider");
    const {
      color,
      horizontalDirection,
      parsedTicks,
      rounded,
      showTicks,
      tickSize,
      trackColor,
      trackFillColor,
      trackSize,
      vertical,
      min,
      max
    } = slider;
    const {
      roundedClasses
    } = useRounded(rounded);
    const {
      backgroundColorClasses: trackFillColorClasses,
      backgroundColorStyles: trackFillColorStyles
    } = useBackgroundColor(trackFillColor);
    const {
      backgroundColorClasses: trackColorClasses,
      backgroundColorStyles: trackColorStyles
    } = useBackgroundColor(trackColor);
    const startDir = computed(() => `inset-${vertical.value ? "block-end" : "inline-start"}`);
    const endDir = computed(() => vertical.value ? "height" : "width");
    const backgroundStyles = computed(() => {
      return {
        [startDir.value]: "0%",
        [endDir.value]: "100%"
      };
    });
    const trackFillWidth = computed(() => props.stop - props.start);
    const trackFillStyles = computed(() => {
      return {
        [startDir.value]: convertToUnit(props.start, "%"),
        [endDir.value]: convertToUnit(trackFillWidth.value, "%")
      };
    });
    const computedTicks = computed(() => {
      const ticks = vertical.value ? parsedTicks.value.slice().reverse() : parsedTicks.value;
      return ticks.map((tick, index) => {
        var _a2, _b;
        const directionProperty = vertical.value ? "bottom" : "margin-inline-start";
        const directionValue = tick.value !== min.value && tick.value !== max.value ? convertToUnit(tick.position, "%") : void 0;
        return createVNode("div", {
          "key": tick.value,
          "class": ["v-slider-track__tick", {
            "v-slider-track__tick--filled": tick.position >= props.start && tick.position <= props.stop,
            "v-slider-track__tick--first": tick.value === min.value,
            "v-slider-track__tick--last": tick.value === max.value
          }],
          "style": {
            [directionProperty]: directionValue
          }
        }, [(tick.label || slots["tick-label"]) && createVNode("div", {
          "class": "v-slider-track__tick-label"
        }, [(_b = (_a2 = slots["tick-label"]) == null ? void 0 : _a2.call(slots, {
          tick,
          index
        })) != null ? _b : tick.label])]);
      });
    });
    useRender(() => {
      return createVNode("div", {
        "class": ["v-slider-track", roundedClasses.value],
        "style": {
          "--v-slider-track-size": convertToUnit(trackSize.value),
          "--v-slider-tick-size": convertToUnit(tickSize.value),
          direction: !vertical.value ? horizontalDirection.value : void 0
        }
      }, [createVNode("div", {
        "class": ["v-slider-track__background", trackColorClasses.value, {
          "v-slider-track__background--opacity": !!color.value || !trackFillColor.value
        }],
        "style": {
          ...backgroundStyles.value,
          ...trackColorStyles.value
        }
      }, null), createVNode("div", {
        "class": ["v-slider-track__fill", trackFillColorClasses.value],
        "style": {
          ...trackFillStyles.value,
          ...trackFillColorStyles.value
        }
      }, null), showTicks.value && createVNode("div", {
        "class": ["v-slider-track__ticks", {
          "v-slider-track__ticks--always-show": showTicks.value === "always"
        }]
      }, [computedTicks.value])]);
    });
    return {};
  }
});
const VSlider = genericComponent()({
  name: "VSlider",
  props: {
    ...makeFocusProps(),
    ...makeSliderProps(),
    ...makeVInputProps(),
    modelValue: {
      type: [Number, String],
      default: 0
    }
  },
  emits: {
    "update:focused": (value) => true,
    "update:modelValue": (v) => true
  },
  setup(props, _ref) {
    let {
      slots
    } = _ref;
    const thumbContainerRef = ref();
    const {
      min,
      max,
      mousePressed,
      roundValue,
      onSliderMousedown,
      onSliderTouchstart,
      trackContainerRef,
      position,
      hasLabels,
      readonly
    } = useSlider({
      props,
      handleSliderMouseUp: (newValue) => model.value = roundValue(newValue),
      handleMouseMove: (newValue) => model.value = roundValue(newValue),
      getActiveThumb: () => {
        var _a2;
        return (_a2 = thumbContainerRef.value) == null ? void 0 : _a2.$el;
      }
    });
    const model = useProxiedModel(props, "modelValue", void 0, (v) => {
      const value = typeof v === "string" ? parseFloat(v) : v == null ? min.value : v;
      return roundValue(value);
    });
    const {
      isFocused,
      focus,
      blur
    } = useFocus(props);
    const trackStop = computed(() => position(model.value));
    useRender(() => {
      const [inputProps, _] = filterInputProps(props);
      const hasPrepend = !!(props.label || slots.label || slots.prepend);
      return createVNode(VInput, mergeProps({
        "class": ["v-slider", {
          "v-slider--has-labels": !!slots["tick-label"] || hasLabels.value,
          "v-slider--focused": isFocused.value,
          "v-slider--pressed": mousePressed.value,
          "v-slider--disabled": props.disabled
        }]
      }, inputProps, {
        "focused": isFocused.value
      }), {
        ...slots,
        prepend: hasPrepend ? (slotProps) => {
          var _a2, _b, _c;
          return createVNode(Fragment, null, [((_b = (_a2 = slots.label) == null ? void 0 : _a2.call(slots, slotProps)) != null ? _b : props.label) ? createVNode(VLabel, {
            "id": slotProps.id,
            "class": "v-slider__label",
            "text": props.label
          }, null) : void 0, (_c = slots.prepend) == null ? void 0 : _c.call(slots, slotProps)]);
        } : void 0,
        default: (_ref2) => {
          let {
            id,
            messagesId
          } = _ref2;
          return createVNode("div", {
            "class": "v-slider__container",
            "onMousedown": !readonly.value ? onSliderMousedown : void 0,
            "onTouchstartPassive": !readonly.value ? onSliderTouchstart : void 0
          }, [createVNode("input", {
            "id": id.value,
            "name": props.name || id.value,
            "disabled": props.disabled,
            "readonly": props.readonly,
            "tabindex": "-1",
            "value": model.value
          }, null), createVNode(VSliderTrack, {
            "ref": trackContainerRef,
            "start": 0,
            "stop": trackStop.value
          }, {
            "tick-label": slots["tick-label"]
          }), createVNode(VSliderThumb, {
            "ref": thumbContainerRef,
            "aria-describedby": messagesId.value,
            "focused": isFocused.value,
            "min": min.value,
            "max": max.value,
            "modelValue": model.value,
            "onUpdate:modelValue": (v) => model.value = v,
            "position": trackStop.value,
            "elevation": props.elevation,
            "onFocus": focus,
            "onBlur": blur
          }, {
            "thumb-label": slots["thumb-label"]
          })]);
        }
      });
    });
    return {};
  }
});
const VColorPickerPreview = defineComponent({
  name: "VColorPickerPreview",
  props: {
    color: {
      type: Object
    },
    disabled: Boolean,
    hideAlpha: Boolean
  },
  emits: {
    "update:color": (color) => true
  },
  setup(props, _ref) {
    let {
      emit
    } = _ref;
    useRender(() => {
      var _a2, _b, _c, _d;
      return createVNode("div", {
        "class": ["v-color-picker-preview", {
          "v-color-picker-preview--hide-alpha": props.hideAlpha
        }]
      }, [createVNode("div", {
        "class": "v-color-picker-preview__dot"
      }, [createVNode("div", {
        "style": {
          background: HSVtoCSS((_a2 = props.color) != null ? _a2 : nullColor)
        }
      }, null)]), createVNode("div", {
        "class": "v-color-picker-preview__sliders"
      }, [createVNode(VSlider, {
        "class": "v-color-picker-preview__track v-color-picker-preview__hue",
        "modelValue": (_b = props.color) == null ? void 0 : _b.h,
        "onUpdate:modelValue": (h2) => {
          var _a3;
          return emit("update:color", {
            ...(_a3 = props.color) != null ? _a3 : nullColor,
            h: h2
          });
        },
        "step": 0,
        "min": 0,
        "max": 360,
        "disabled": props.disabled,
        "thumbSize": 14,
        "trackSize": 8,
        "trackFillColor": "white",
        "hideDetails": true
      }, null), !props.hideAlpha && createVNode(VSlider, {
        "class": "v-color-picker-preview__track v-color-picker-preview__alpha",
        "modelValue": (_d = (_c = props.color) == null ? void 0 : _c.a) != null ? _d : 1,
        "onUpdate:modelValue": (a) => {
          var _a3;
          return emit("update:color", {
            ...(_a3 = props.color) != null ? _a3 : nullColor,
            a
          });
        },
        "step": 1 / 256,
        "min": 0,
        "max": 1,
        "disabled": props.disabled,
        "thumbSize": 14,
        "trackSize": 8,
        "trackFillColor": "white",
        "hideDetails": true
      }, null)])]);
    });
    return {};
  }
});
var VColorPickerSwatches$1 = "";
const red = Object.freeze({
  base: "#f44336",
  lighten5: "#ffebee",
  lighten4: "#ffcdd2",
  lighten3: "#ef9a9a",
  lighten2: "#e57373",
  lighten1: "#ef5350",
  darken1: "#e53935",
  darken2: "#d32f2f",
  darken3: "#c62828",
  darken4: "#b71c1c",
  accent1: "#ff8a80",
  accent2: "#ff5252",
  accent3: "#ff1744",
  accent4: "#d50000"
});
const pink = Object.freeze({
  base: "#e91e63",
  lighten5: "#fce4ec",
  lighten4: "#f8bbd0",
  lighten3: "#f48fb1",
  lighten2: "#f06292",
  lighten1: "#ec407a",
  darken1: "#d81b60",
  darken2: "#c2185b",
  darken3: "#ad1457",
  darken4: "#880e4f",
  accent1: "#ff80ab",
  accent2: "#ff4081",
  accent3: "#f50057",
  accent4: "#c51162"
});
const purple = Object.freeze({
  base: "#9c27b0",
  lighten5: "#f3e5f5",
  lighten4: "#e1bee7",
  lighten3: "#ce93d8",
  lighten2: "#ba68c8",
  lighten1: "#ab47bc",
  darken1: "#8e24aa",
  darken2: "#7b1fa2",
  darken3: "#6a1b9a",
  darken4: "#4a148c",
  accent1: "#ea80fc",
  accent2: "#e040fb",
  accent3: "#d500f9",
  accent4: "#aa00ff"
});
const deepPurple = Object.freeze({
  base: "#673ab7",
  lighten5: "#ede7f6",
  lighten4: "#d1c4e9",
  lighten3: "#b39ddb",
  lighten2: "#9575cd",
  lighten1: "#7e57c2",
  darken1: "#5e35b1",
  darken2: "#512da8",
  darken3: "#4527a0",
  darken4: "#311b92",
  accent1: "#b388ff",
  accent2: "#7c4dff",
  accent3: "#651fff",
  accent4: "#6200ea"
});
const indigo = Object.freeze({
  base: "#3f51b5",
  lighten5: "#e8eaf6",
  lighten4: "#c5cae9",
  lighten3: "#9fa8da",
  lighten2: "#7986cb",
  lighten1: "#5c6bc0",
  darken1: "#3949ab",
  darken2: "#303f9f",
  darken3: "#283593",
  darken4: "#1a237e",
  accent1: "#8c9eff",
  accent2: "#536dfe",
  accent3: "#3d5afe",
  accent4: "#304ffe"
});
const blue = Object.freeze({
  base: "#2196f3",
  lighten5: "#e3f2fd",
  lighten4: "#bbdefb",
  lighten3: "#90caf9",
  lighten2: "#64b5f6",
  lighten1: "#42a5f5",
  darken1: "#1e88e5",
  darken2: "#1976d2",
  darken3: "#1565c0",
  darken4: "#0d47a1",
  accent1: "#82b1ff",
  accent2: "#448aff",
  accent3: "#2979ff",
  accent4: "#2962ff"
});
const lightBlue = Object.freeze({
  base: "#03a9f4",
  lighten5: "#e1f5fe",
  lighten4: "#b3e5fc",
  lighten3: "#81d4fa",
  lighten2: "#4fc3f7",
  lighten1: "#29b6f6",
  darken1: "#039be5",
  darken2: "#0288d1",
  darken3: "#0277bd",
  darken4: "#01579b",
  accent1: "#80d8ff",
  accent2: "#40c4ff",
  accent3: "#00b0ff",
  accent4: "#0091ea"
});
const cyan = Object.freeze({
  base: "#00bcd4",
  lighten5: "#e0f7fa",
  lighten4: "#b2ebf2",
  lighten3: "#80deea",
  lighten2: "#4dd0e1",
  lighten1: "#26c6da",
  darken1: "#00acc1",
  darken2: "#0097a7",
  darken3: "#00838f",
  darken4: "#006064",
  accent1: "#84ffff",
  accent2: "#18ffff",
  accent3: "#00e5ff",
  accent4: "#00b8d4"
});
const teal = Object.freeze({
  base: "#009688",
  lighten5: "#e0f2f1",
  lighten4: "#b2dfdb",
  lighten3: "#80cbc4",
  lighten2: "#4db6ac",
  lighten1: "#26a69a",
  darken1: "#00897b",
  darken2: "#00796b",
  darken3: "#00695c",
  darken4: "#004d40",
  accent1: "#a7ffeb",
  accent2: "#64ffda",
  accent3: "#1de9b6",
  accent4: "#00bfa5"
});
const green = Object.freeze({
  base: "#4caf50",
  lighten5: "#e8f5e9",
  lighten4: "#c8e6c9",
  lighten3: "#a5d6a7",
  lighten2: "#81c784",
  lighten1: "#66bb6a",
  darken1: "#43a047",
  darken2: "#388e3c",
  darken3: "#2e7d32",
  darken4: "#1b5e20",
  accent1: "#b9f6ca",
  accent2: "#69f0ae",
  accent3: "#00e676",
  accent4: "#00c853"
});
const lightGreen = Object.freeze({
  base: "#8bc34a",
  lighten5: "#f1f8e9",
  lighten4: "#dcedc8",
  lighten3: "#c5e1a5",
  lighten2: "#aed581",
  lighten1: "#9ccc65",
  darken1: "#7cb342",
  darken2: "#689f38",
  darken3: "#558b2f",
  darken4: "#33691e",
  accent1: "#ccff90",
  accent2: "#b2ff59",
  accent3: "#76ff03",
  accent4: "#64dd17"
});
const lime = Object.freeze({
  base: "#cddc39",
  lighten5: "#f9fbe7",
  lighten4: "#f0f4c3",
  lighten3: "#e6ee9c",
  lighten2: "#dce775",
  lighten1: "#d4e157",
  darken1: "#c0ca33",
  darken2: "#afb42b",
  darken3: "#9e9d24",
  darken4: "#827717",
  accent1: "#f4ff81",
  accent2: "#eeff41",
  accent3: "#c6ff00",
  accent4: "#aeea00"
});
const yellow = Object.freeze({
  base: "#ffeb3b",
  lighten5: "#fffde7",
  lighten4: "#fff9c4",
  lighten3: "#fff59d",
  lighten2: "#fff176",
  lighten1: "#ffee58",
  darken1: "#fdd835",
  darken2: "#fbc02d",
  darken3: "#f9a825",
  darken4: "#f57f17",
  accent1: "#ffff8d",
  accent2: "#ffff00",
  accent3: "#ffea00",
  accent4: "#ffd600"
});
const amber = Object.freeze({
  base: "#ffc107",
  lighten5: "#fff8e1",
  lighten4: "#ffecb3",
  lighten3: "#ffe082",
  lighten2: "#ffd54f",
  lighten1: "#ffca28",
  darken1: "#ffb300",
  darken2: "#ffa000",
  darken3: "#ff8f00",
  darken4: "#ff6f00",
  accent1: "#ffe57f",
  accent2: "#ffd740",
  accent3: "#ffc400",
  accent4: "#ffab00"
});
const orange = Object.freeze({
  base: "#ff9800",
  lighten5: "#fff3e0",
  lighten4: "#ffe0b2",
  lighten3: "#ffcc80",
  lighten2: "#ffb74d",
  lighten1: "#ffa726",
  darken1: "#fb8c00",
  darken2: "#f57c00",
  darken3: "#ef6c00",
  darken4: "#e65100",
  accent1: "#ffd180",
  accent2: "#ffab40",
  accent3: "#ff9100",
  accent4: "#ff6d00"
});
const deepOrange = Object.freeze({
  base: "#ff5722",
  lighten5: "#fbe9e7",
  lighten4: "#ffccbc",
  lighten3: "#ffab91",
  lighten2: "#ff8a65",
  lighten1: "#ff7043",
  darken1: "#f4511e",
  darken2: "#e64a19",
  darken3: "#d84315",
  darken4: "#bf360c",
  accent1: "#ff9e80",
  accent2: "#ff6e40",
  accent3: "#ff3d00",
  accent4: "#dd2c00"
});
const brown = Object.freeze({
  base: "#795548",
  lighten5: "#efebe9",
  lighten4: "#d7ccc8",
  lighten3: "#bcaaa4",
  lighten2: "#a1887f",
  lighten1: "#8d6e63",
  darken1: "#6d4c41",
  darken2: "#5d4037",
  darken3: "#4e342e",
  darken4: "#3e2723"
});
const blueGrey = Object.freeze({
  base: "#607d8b",
  lighten5: "#eceff1",
  lighten4: "#cfd8dc",
  lighten3: "#b0bec5",
  lighten2: "#90a4ae",
  lighten1: "#78909c",
  darken1: "#546e7a",
  darken2: "#455a64",
  darken3: "#37474f",
  darken4: "#263238"
});
const grey = Object.freeze({
  base: "#9e9e9e",
  lighten5: "#fafafa",
  lighten4: "#f5f5f5",
  lighten3: "#eeeeee",
  lighten2: "#e0e0e0",
  lighten1: "#bdbdbd",
  darken1: "#757575",
  darken2: "#616161",
  darken3: "#424242",
  darken4: "#212121"
});
const shades = Object.freeze({
  black: "#000000",
  white: "#ffffff",
  transparent: "#ffffff00"
});
var colors = Object.freeze({
  red,
  pink,
  purple,
  deepPurple,
  indigo,
  blue,
  lightBlue,
  cyan,
  teal,
  green,
  lightGreen,
  lime,
  yellow,
  amber,
  orange,
  deepOrange,
  brown,
  blueGrey,
  grey,
  shades
});
function parseDefaultColors(colors2) {
  return Object.keys(colors2).map((key) => {
    const color = colors2[key];
    return color.base ? [color.base, color.darken4, color.darken3, color.darken2, color.darken1, color.lighten1, color.lighten2, color.lighten3, color.lighten4, color.lighten5] : [color.black, color.white, color.transparent];
  });
}
const VColorPickerSwatches = defineComponent({
  name: "VColorPickerSwatches",
  props: {
    swatches: {
      type: Array,
      default: () => parseDefaultColors(colors)
    },
    disabled: Boolean,
    color: Object,
    maxHeight: [Number, String]
  },
  emits: {
    "update:color": (color) => true
  },
  setup(props, _ref) {
    let {
      emit
    } = _ref;
    useRender(() => createVNode("div", {
      "class": "v-color-picker-swatches",
      "style": {
        maxHeight: convertToUnit(props.maxHeight)
      }
    }, [createVNode("div", null, [props.swatches.map((swatch) => createVNode("div", {
      "class": "v-color-picker-swatches__swatch"
    }, [swatch.map((color) => {
      const hsva = parseColor(color);
      return createVNode("div", {
        "class": "v-color-picker-swatches__color",
        "onClick": () => hsva && emit("update:color", hsva)
      }, [createVNode("div", {
        "style": {
          background: color
        }
      }, [props.color && deepEqual(props.color, hsva) ? createVNode(VIcon, {
        "size": "x-small",
        "icon": "$success",
        "color": getContrast(color, "#FFFFFF") > 2 ? "white" : "black"
      }, null) : void 0])]);
    })]))])]));
    return {};
  }
});
var VSheet$1 = "";
const makeVSheetProps = propsFactory({
  color: String,
  ...makeBorderProps(),
  ...makeDimensionProps(),
  ...makeElevationProps(),
  ...makeLocationProps(),
  ...makePositionProps(),
  ...makeRoundedProps(),
  ...makeTagProps(),
  ...makeThemeProps()
}, "v-sheet");
const VSheet = genericComponent()({
  name: "VSheet",
  props: {
    ...makeVSheetProps()
  },
  setup(props, _ref) {
    let {
      slots
    } = _ref;
    const {
      themeClasses
    } = provideTheme(props);
    const {
      backgroundColorClasses,
      backgroundColorStyles
    } = useBackgroundColor(toRef(props, "color"));
    const {
      borderClasses
    } = useBorder(props);
    const {
      dimensionStyles
    } = useDimension(props);
    const {
      elevationClasses
    } = useElevation(props);
    const {
      locationStyles
    } = useLocation(props);
    const {
      positionClasses
    } = usePosition(props);
    const {
      roundedClasses
    } = useRounded(props);
    useRender(() => createVNode(props.tag, {
      "class": ["v-sheet", themeClasses.value, backgroundColorClasses.value, borderClasses.value, elevationClasses.value, positionClasses.value, roundedClasses.value],
      "style": [backgroundColorStyles.value, dimensionStyles.value, locationStyles.value]
    }, slots));
    return {};
  }
});
const VColorPicker = defineComponent({
  name: "VColorPicker",
  props: {
    canvasHeight: {
      type: [String, Number],
      default: 150
    },
    disabled: Boolean,
    dotSize: {
      type: [Number, String],
      default: 10
    },
    hideCanvas: Boolean,
    hideSliders: Boolean,
    hideInputs: Boolean,
    mode: {
      type: String,
      default: "rgba",
      validator: (v) => Object.keys(modes).includes(v)
    },
    modes: {
      type: Array,
      default: () => Object.keys(modes),
      validator: (v) => Array.isArray(v) && v.every((m) => Object.keys(modes).includes(m))
    },
    showSwatches: Boolean,
    swatches: Array,
    swatchesMaxHeight: {
      type: [Number, String],
      default: 150
    },
    modelValue: {
      type: [Object, String]
    },
    width: {
      type: [Number, String],
      default: 300
    },
    ...makeElevationProps(),
    ...makeRoundedProps(),
    ...makeThemeProps()
  },
  emits: {
    "update:modelValue": (color) => true,
    "update:mode": (mode) => true
  },
  setup(props) {
    const mode = useProxiedModel(props, "mode");
    const lastPickedColor = ref(null);
    const currentColor = useProxiedModel(props, "modelValue", void 0, (v) => {
      let c = parseColor(v);
      if (!c)
        return null;
      if (lastPickedColor.value) {
        c = {
          ...c,
          h: lastPickedColor.value.h
        };
        lastPickedColor.value = null;
      }
      return c;
    }, (v) => {
      if (!v)
        return null;
      return extractColor(v, props.modelValue);
    });
    const updateColor = (hsva) => {
      currentColor.value = hsva;
      lastPickedColor.value = hsva;
    };
    onMounted(() => {
      if (!props.modes.includes(mode.value))
        mode.value = props.modes[0];
    });
    provideDefaults({
      VSlider: {
        color: void 0,
        trackColor: void 0,
        trackFillColor: void 0
      }
    });
    useRender(() => {
      var _a2;
      return createVNode(VSheet, {
        "rounded": props.rounded,
        "elevation": props.elevation,
        "theme": props.theme,
        "class": ["v-color-picker"],
        "style": {
          "--v-color-picker-color-hsv": HSVtoCSS({
            ...(_a2 = currentColor.value) != null ? _a2 : nullColor,
            a: 1
          })
        },
        "maxWidth": props.width
      }, {
        default: () => [!props.hideCanvas && createVNode(VColorPickerCanvas, {
          "key": "canvas",
          "color": currentColor.value,
          "onUpdate:color": updateColor,
          "disabled": props.disabled,
          "dotSize": props.dotSize,
          "width": props.width,
          "height": props.canvasHeight
        }, null), (!props.hideSliders || !props.hideInputs) && createVNode("div", {
          "key": "controls",
          "class": "v-color-picker__controls"
        }, [!props.hideSliders && createVNode(VColorPickerPreview, {
          "key": "preview",
          "color": currentColor.value,
          "onUpdate:color": updateColor,
          "hideAlpha": !mode.value.endsWith("a"),
          "disabled": props.disabled
        }, null), !props.hideInputs && createVNode(VColorPickerEdit, {
          "key": "edit",
          "modes": props.modes,
          "mode": mode.value,
          "onUpdate:mode": (m) => mode.value = m,
          "color": currentColor.value,
          "onUpdate:color": updateColor,
          "disabled": props.disabled
        }, null)]), props.showSwatches && createVNode(VColorPickerSwatches, {
          "key": "swatches",
          "color": currentColor.value,
          "onUpdate:color": updateColor,
          "maxHeight": props.swatchesMaxHeight,
          "swatches": props.swatches,
          "disabled": props.disabled
        }, null)]
      });
    });
    return {};
  }
});
export { VColorPicker as V };
