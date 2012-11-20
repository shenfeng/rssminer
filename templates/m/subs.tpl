<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-height, initial-scale=1.0, user-scalable=no" />
    <title>Rssminer</title>
    <link href="/s/css/m.css" rel="stylesheet" type="text/css" />
  </head>
  <body>
    <ul class="lists">
      {{#subs}}
        <li>
          <!-- <img src="//192.168.1.200:9090/fav?h=moc.ativgi.www" width="32" height="32"> -->
          <p>
            <a href="/m/{{id}}"> {{ title }} </a>
          </p>
          <div class="meta">
            <span>猜你喜欢{{ like }}</span>
            <span>共{{ total }}</span>
          </div>
        </li>
      {{/subs}}
    </ul>
  </body>
  <!-- <script src="/s/js/lib/zepto.js"></script> -->
  <!-- <script src="/s/js/rssminer/mobile.js"></script> -->
</html>
