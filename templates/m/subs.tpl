<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no" />
    <title>{{m-sub-list}}</title>
    <link href="/s/css/m.css" rel="stylesheet" type="text/css" />
  </head>
  <body>
    <h2>{{m-sub-list}}</h2>
    <ul class="lists">
      {{#subs}}
        <li>
          <p>
            <a href="/m/{{id}}"> {{ title }} </a>
          </p>
          <div class="meta">
            <i>猜你喜欢{{ like }}</i>
            <i>共{{ total }}</i>
          </div>
        </li>
      {{/subs}}
    </ul>
  </body>
  <!-- <script src="/s/js/lib/zepto.js"></script> -->
  <!-- <script src="/s/js/rssminer/mobile.js"></script> -->
</html>
