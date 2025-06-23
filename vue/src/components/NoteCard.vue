<template>
  <div class="note-card">
    <img class="cover" :src="note.image" alt="封面图" />
    <div class="note-content">
      <h3 class="title">{{ note.title }}</h3>
      <p class="description">{{ note.description }}</p>
      <div class="author">
        <img class="avatar" :src="note.avatar" alt="作者头像" />
        <span>{{ note.author }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  note: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.note-card {
  width: 260px;
  background: #ffffff;
  border-radius: 1rem;
  overflow: hidden;
  box-shadow: 0 4px 10px rgba(180, 220, 210, 0.2);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  display: flex;
  flex-direction: column;
}

.note-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(160, 200, 180, 0.3);
}

.cover {
  width: 100%;
  height: 160px;
  object-fit: cover;
}

.note-content {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.title {
  font-size: 1.1rem;
  color: #4a7d6d;
  font-weight: 600;
  line-height: 1.3;
}

.description {
  font-size: 0.9rem;
  color: #777;
  line-height: 1.4;
}

.author {
  display: flex;
  align-items: center;
  margin-top: auto;
  gap: 8px;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}
</style>
