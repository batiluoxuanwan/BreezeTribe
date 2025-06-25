<template>
    <div class="create-package" style="padding: 24px; max-width: 900px; margin: auto;">
    <h2>发布新的旅游团</h2>
  
    <!-- 目的地输入 -->
    <el-form :inline="true" label-width="80px" style="margin-bottom: 12px;">
    <el-form-item label="📍目的地">
      <el-autocomplete
        v-model="destination"
        :fetch-suggestions="querySearchdestination"
        placeholder="请输入目的地"
        clearable
        :prefix-icon="Search"
        style="width: 400px"
        @select="handleSelect"
      />
    </el-form-item>
    </el-form>
          
    <!-- 时间选择 -->
    <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 24px;">
    <el-form-item label="出发时间" label-width="80px" style="margin-bottom: 0;">
        <el-date-picker v-model="startDate" type="date" placeholder="开始日期" />
    </el-form-item>

    <el-form-item label="返回时间" label-width="80px" style="margin-bottom: 0;">
        <el-date-picker v-model="endDate" type="date" placeholder="结束日期" />
    </el-form-item>

    <el-button type="primary" @click="generateDays">生成行程</el-button>
    </div>

    <div v-for="(day, index) in days" :key="index" class="day-card">
      <h3>DAY {{ index + 1 }}</h3>
      <el-button type="primary" @click="spotDialogVisible = true">+ 添加地点/活动</el-button>

    <!--景点搜索框-->
      <el-dialog v-model="spotDialogVisible" title="搜索景点" width="600px">
        <el-autocomplete
            v-model="spotKeyword"
            :fetch-suggestions="querySearchSpots"
            placeholder="输入关键词搜索"
            clearable
            @select="handleSpotSelect"
            style="width: 100%"
        />

        <div v-if="spotResults.length" style="margin-top: 20px;">
            <el-card
            v-for="(item, index) in spotResults"
            :key="index"
            class="result-card"
            shadow="hover"
            @click="handleSpotSelect(item)"
            style="margin-bottom: 10px; cursor: pointer;"
            >
            <div><strong>{{ item.name }}</strong></div>
            <div style="font-size: 12px; color: gray;">
                {{ item.province }} {{ item.city }} {{ item.district }}
            </div>
            </el-card>
        </div>
        <div v-if="loading" style="margin-top: 20px;">搜索中...</div>

        <template #footer>
            <el-button @click="spotDialogVisible = false">取消</el-button>
        </template>
        </el-dialog>

        <!-- 景点卡片列表 -->
        <div class="spot-list">
            <el-card
            v-for="(spot, i) in day.spots"
            :key="i"
            class="spot-card"
            shadow="hover"
            >
            <!-- 景点名称 -->
            <div class="spot-title">{{ spot.name }}</div>

            <!-- 备注预览框 -->
            <div class="note-preview" @click="spot.editing = true">
                <el-icon><Edit /></el-icon>
                <div class="note-content">
                <div v-if="spot.timeRange.length">{{ spot.timeRange[0] }} - {{ spot.timeRange[1] }}</div>
                <div v-if="spot.note">{{ spot.note }}</div>
                <el-image
                    v-if="spot.imageUrl"
                    :src="spot.imageUrl"
                    :preview-src-list="[spot.imageUrl]"
                    style="width: 60px; height: 60px"
                />
                <div v-if="!spot.note && !spot.timeRange.length && !spot.imageUrl" class="empty-note">
                    点击添加备注
                </div>
                </div>
            </div>

            <!-- 备注弹窗 -->
            <el-dialog v-model="spot.editing" :title="spot.name" width="400px">
                <el-input
                v-model="spot.note"
                type="textarea"
                placeholder="输入备注"
                rows="3"
                style="margin-bottom: 12px"
                />
                <el-time-picker
                is-range
                v-model="spot.timeRange"
                range-separator="至"
                start-placeholder="开始"
                end-placeholder="结束"
                format="HH:mm"
                value-format="HH:mm"
                style="width: 100%; margin-bottom: 12px"
                />
                <el-upload
                class="upload-demo"
                action="#"
                :auto-upload="false"
                :on-change="(file) => handleImageUpload(file, day, i)"
                >
                <el-button icon="Upload" size="small">添加图片</el-button>
                </el-upload>
                <template #footer>
                <el-button @click="spot.editing = false">完成</el-button>
                </template>
            </el-dialog>
            </el-card>
        </div>

    </div>
    
  
    </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Search, Edit, Upload } from '@element-plus/icons-vue'
import {publicAxios} from '@/utils/request'

const destination = ref('')

const startDate = ref(null)
const endDate = ref(null)
const days = ref([])

const spotDialogVisible = ref(false)
const spotKeyword = ref('')
const spotResults = ref([])
const loading = ref(false)

const cities = [
  { name: '北京', province: '北京市', city: '北京市', district: '' },
  { name: '上海', province: '上海市', city: '上海市', district: '' },
  { name: '武汉', province: '湖北省', city: '武汉市', district: '' },
  { name: '广州', province: '广东省', city: '广州市', district: '' },
  { name: '深圳', province: '广东省', city: '深圳市', district: '' },
  { name: '南京', province: '江苏省', city: '南京市', district: '' }
]

const mockSpots = [
  { name: '故宫', province: '北京市', city: '北京市', district: '东城区' },
  { name: '外滩', province: '上海市', city: '上海市', district: '黄浦区' },
  { name: '黄鹤楼', province: '湖北省', city: '武汉市', district: '武昌区' },
  { name: '南京路', province: '上海市', city: '上海市', district: '黄浦区' }
]
// //模拟搜索
// function querySearchdestination(queryString, cb) {
//   if (!queryString) {
//     cb([])
//     return
//   }
//   const results = cities.filter(city =>
//     city.name.includes(queryString) || city.province.includes(queryString)
//   ).map(city => ({
//     value: city.name,
//     ...city
//   }))
//   cb(results)
// }

const querySearchdestination = async (queryString, cb) => {
  if (!queryString) return cb([])
  try {
    const res = await publicAxios.get('/public/spots/suggestions', {
      params: {
        keyword: queryString,
        region: '中国' // 或你设定的默认值
      }
    })
    const results = res.data?.data || []
    // el-autocomplete 要求数组中的对象有 `value` 字段
    cb(results.map(item => ({ ...item, value: item.name })))
  } catch (err) {
    console.error('获取目的地建议失败', err)
    cb([])
  }
}

const handleSelect = (item) => {
  destination.value = item.name
  // 可以存储经纬度、UID 之类信息
  console.log('选中目的地:', item)
}

const generateDays = () => {
  if (!startDate.value || !endDate.value) return
  const start = new Date(startDate.value)
  const end = new Date(endDate.value)
  const diff = Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1
  days.value = Array.from({ length: diff }, () => ({ spots: [] }))
}
//模拟搜索景点
function querySearchSpots(query, cb) {
  if (!query) return cb([])
  loading.value = true
  setTimeout(() => {
    const results = mockSpots.filter(spot =>
      spot.name.includes(query) || spot.city.includes(query)
    )
    spotResults.value = results
    loading.value = false
    cb(results.map(r => ({ value: r.name, ...r })))
  }, 500)
}

const handleSpotSelect = (item) => {
  if (activeDayIndex.value == null) return;

  days.value[activeDayIndex.value].spots.push({
    name: item.name,
    note: '',
    timeRange: [],
    imageUrl: '',
    editing: false,
  });

  spotDialogVisible.value = false;
  spotKeyword.value = '';
  spotResults.value = [];
};

function openSpotDialog(dayIndex) {
  currentDayIndex.value = dayIndex
  spotKeyword.value = ''
  spotResults.value = []
  spotDialogVisible.value = true
}

const handleImageUpload = (file, day, i) => {
  const reader = new FileReader()
  reader.onload = () => {
    day.spots[i].imageUrl = reader.result
  }
  reader.readAsDataURL(file.raw)
}
</script>

<style scoped>
.day-card {
  margin-bottom: 24px;
  border: 1px solid #eee;
  padding: 16px;
  border-radius: 8px;
}
.spot-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}
.spot-card {
  width: 300px;
  padding: 12px;
  border-radius: 8px;
}
.spot-title {
  font-weight: bold;
  margin-bottom: 8px;
}
.note-preview {
  display: flex;
  gap: 8px;
  background-color: #f9f9f9;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
}
.note-content {
  flex: 1;
  font-size: 14px;
}
.empty-note {
  color: #aaa;
  font-style: italic;
}
</style>
