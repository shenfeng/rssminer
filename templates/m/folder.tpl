<!doctype html>
<html>
  <head>
    {{>partials/m_header}}
    <link href="/s/css/m.css?{VERSION}" rel="stylesheet" type="text/css" />
  </head>
  <script>function f_e (t) { t.src = "/s/imgs/16px-feed-icon.png"; t.onerror = null; }</script>
  <body>
    <h2> <a href="/m">ALL</a> - {{folder}}</h2>
    <ul class="lists">
      {{#subs}}
        <li>
          <p>
            <img src="{{{static-server}}}/fav?h={{{host}}}" onerror="f_e(this)"/>
            <a href="/m/{{id}}/latest"> {{ title }} </a>
          </p>
          <div class="meta">
            <i class="total">共{{ total }}篇文章</i>
            {{#like?}}
              <a href="/m/{{id}}/likest">猜你喜欢{{ like }}</a>
            {{/like?}}
          </div>
        </li>
      {{/subs}}
    </ul>
  </body>
</html>
