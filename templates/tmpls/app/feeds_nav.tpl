<h3 title="{{title}}">
  {{#url}}
    <a target="blank" title="{{title}}" href="{{url}}">{{title}}</a>
  {{/url}}
  {{^url}}
  {{ title }}
  {{/url}}
</h3>
<ul id="feed-list" class="feeds">
  {{>feeds_list}}
</ul>
{{#pager}}
  <img class="loader" src="/imgs/loader-15.gif"/>
{{/pager}}
