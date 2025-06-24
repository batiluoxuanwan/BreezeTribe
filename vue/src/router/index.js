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
    }
];

const router = createRouter({
    routes,
    history:createWebHistory(),
})

export default router