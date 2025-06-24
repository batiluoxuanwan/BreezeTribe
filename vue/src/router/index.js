import { createRouter, createWebHistory } from 'vue-router'

const routes =[
    {
        path:'/',
        name:'首页',
        component:() =>import('../views/Public/home.vue')
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
        path:'/user/me',
        name:'用户个人主页',
        component:() =>import('../views/User/Profile.vue')
    },
    {
        path:'/merchant/me',
        name:'团长个人主页',
        component:() =>import('../views/Merchant/Profile.vue')
    },
    {
        path:'/administrator/me',
        name:'管理员个人主页',
        component:() =>import('../views/Administrator/Profile.vue')
    }
];

const router = createRouter({
    routes,
    history:createWebHistory(),
})

export default router