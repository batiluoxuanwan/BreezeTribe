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
        path:'/square',
        name:'旅行广场',
        component:() =>import('../views/Public/Square.vue'),
        props: route => ({ keyword: route.query.keyword, tab: route.query.tab || 'groups', page: Number(route.query.page) || 1 })
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
        path: '/edit-note/:id',
        name: '用户编辑游记',
        component:() =>import('../views/User/PublishTravelNote.vue'),
        props: true, 
        meta: { requiresAuth: true }
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
    {
        path: '/travel-note/:id', 
        name: 'TravelNoteDetail', 
        component:() =>import('../views/Public/TravelNoteDetail.vue'),
        props: true, 
    },
    {
        path: '/my-note/:id', 
        name: 'EditNote', 
        component:() =>import('../views/User/EditNote.vue'),
        props: true, 
    },
    {
        path: '/chat/:friendId',
        name: 'ChatRoom',
        component: () => import('../views/User/ChatRoom.vue'),
        props: true // 开启 props 传参
    },
    {
        path: '/user/friends',
        name: 'FriendList',
        component: () => import('../views/User/FriendList.vue'),
        //props: true // 开启 props 传参
    },

];

const router = createRouter({
    routes,
    history:createWebHistory(),
})

export default router