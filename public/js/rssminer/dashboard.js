$(function(){
  var reader = window.Rssminer,
      tmpl = reader.tmpls,
      ajax = reader.ajax;

  var update = function () {
    var t_rss = _.template(tmpl.rsslinks),
        t_crawler = _.template(tmpl.crawlerlinks);
    ajax.get("/api/dashboard/crawler").done(function (data) {
      $("#page-wrap").empty()
        .append(t_rss(data))
        .append(t_crawler(data));
    });
  };
  update();
});
