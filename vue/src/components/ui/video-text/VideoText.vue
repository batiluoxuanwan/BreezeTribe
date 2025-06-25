<template>
  <div class="relative w-full h-200 overflow-hidden" :class="props.class">
    <div
      class="absolute inset-0 flex items-center justify-center"
      :style="{
        maskImage: dataUrlMask,
        WebkitMaskImage: dataUrlMask,
        WebkitMaskSize: 'cover',
        maskRepeat: 'no-repeat',
        WebkitMaskRepeat: 'no-repeat',
        maskPosition: 'center',
        WebkitMaskPosition: 'center',
      }"
    >
      <video
        class="video"
        :autoplay="autoPlay"
        :muted="muted"
        :loop="loop"
        :preload="preload"
      >
        <source :src="src" />
        Your browser does not support the video tag.
      </video>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, watch, computed,useSlots } from "vue";

const props = defineProps({
  src: { type: String, required: true },
  class: { type: String, required: false, default: "" },
  autoPlay: { type: Boolean, required: false, default: true },
  muted: { type: Boolean, required: false, default: true },
  loop: { type: Boolean, required: false, default: true },
  preload: { type: String, required: false, default: "auto" },
  fontSize: { type: [String, Number], required: false, default: 12 },
  fontWeight: { type: [String, Number], required: false, default: "bold" },
  textAnchor: { type: String, required: false, default: "middle" },
  dominantBaseline: { type: String, required: false, default: "middle" },
  fontFamily: { type: String, required: false, default: "sans-serif" },
});

const defaultSlot = useSlots().default;

const content = computed(
  () =>
    defaultSlot?.()
      .map((vnode) => vnode.children)
      .join("") ?? "",
);

const svgMask = ref("");

const dataUrlMask = computed(
  () => `url("data:image/svg+xml,${encodeURIComponent(svgMask.value)}")`,
);

function updateSvgMask() {
  const responsiveFontSize =
    typeof props.fontSize === "number" ? `${props.fontSize}vw` : props.fontSize;
  svgMask.value = `<svg xmlns='http://www.w3.org/2000/svg' width='100%' height='100%'><text x='50%' y='50%' font-size='${responsiveFontSize}' font-weight='${props.fontWeight}' text-anchor='${props.textAnchor}' dominant-baseline='${props.dominantBaseline}' font-family='${props.fontFamily}'>${content.value}</text></svg>`;
}

watch(content, updateSvgMask);

watch(
  () => [
    props.fontSize,
    props.fontWeight,
    props.textAnchor,
    props.dominantBaseline,
    props.fontFamily,
  ],
  updateSvgMask,
);

onMounted(() => {
  updateSvgMask();
  window.addEventListener("resize", updateSvgMask);
});

onUnmounted(() => {
  window.removeEventListener("resize", updateSvgMask);
});

</script>

<style scoped>
.video{
  height: 100%;
  width: 100%;
  object-fit: cover;
  opacity: 0.85;
}
</style>

