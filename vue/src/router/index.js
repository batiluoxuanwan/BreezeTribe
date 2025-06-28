import { createRouter, createWebHistory } from 'vue-router'

const routes =[
    {
        path:'/',
        name:'首页',
        component:() =>import('../views/Public/Home.vue')
    },
    {
        path:'/login',
        name:'登录',
        component:() =>import('../views/Public/Login.vue')
    },
    {
        path:'/register',
        name:'注册',
        component:() =>import('../views/Public/Register.vue')
    },
    {
        path:'/square',
        name:'旅行广场',
        component:() =>import('../views/Public/Square.vue')
    },
    {
        path:'/user/me',
        name:'用户个人主页',
        component:() =>import('../views/User/Profile.vue')
    },
    {
        path:'/user/publish-travel-note',
        name:'用户发布游记',
        component:() =>import('../views/User/PublishTravelNote.vue')
    },
    {
        path:'/merchant/me',
        name:'团长个人主页',
        component:() =>import('../views/Merchant/Profile.vue')
    },
    {
        path:'/merchant/newgroup',
        name:'发布新团',
        component:() =>import('../views/Merchant/Newgroup.vue')
    },
    {
        path:'/administrator/me',
        name:'管理员个人主页',
        component:() =>import('../views/Administrator/Profile.vue')
    },
    {
        path: '/travel-group/:id', 
        name: 'TravelGroupDetail', 
        component:() =>import('../views/Public/TravelGroupDetail.vue'),
        props: true, 
    },
];

const router = createRouter({
    routes,
    history:createWebHistory(),
})

export default router