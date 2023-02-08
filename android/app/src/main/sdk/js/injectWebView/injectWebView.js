if (navigator.serviceWorker) {
    navigator.serviceWorker.register('serviceWorker.js').then(function(registration) {
        console.log('service worker 注册成功');
    }).catch(function (err) {
        console.log('service worker 注册失败')
    });
}