(function () {
  $(function(){
    $('#slides').slides({
      preload: true,
      preloadImage: 'img/loading.gif',
      play: 5000,
      pause: 2500,
      hoverPause: true
    });
  });
})();
