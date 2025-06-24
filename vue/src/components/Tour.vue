<template>
  <div class="tour-card">
    <img :src="tour.image" alt="tour" class="img" />
    <h3>{{ tour.title }}</h3>
    <p>{{ tour.location }}</p>
    <p class="price">¥{{ tour.price }}起</p>
  </div>
</template>

<script setup>
defineProps({
  tour: Object
})
</script>

<style scoped>
.tour-card {
  width: 200px;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  padding: 10px;
}
.img {
  width: 100%;
  height: 120px;
  object-fit: cover;
}
.price {
  color: red;
}
</style>
