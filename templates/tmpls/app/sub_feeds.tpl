<h2>{{ title }}</h2>
<div class="sort">
  <ul>
    {{#sort}}
      <li {{#selected}} class="selected" {{/selected}}>
        <a href="#{{href}}"> {{text}} </a>
      </li>
    {{/sort}}
  </ul>
</div>
<ul class="feeds">
  {{#feeds}}
    <li class="feed {{cls}}" data-id="{{id}}">
      <a href="#{{href}}">
        <span class="indicator"></span>
        <span class="title">{{title}}</span>
        <span class="author" title="author">{{author}}</span>
        <span class="vote">
          <span class="up" title="I like it"></span>
          <span class="down"></span>
        </span>
        <span class="date">{{ date }}</span>
      </a>
    </li>
  {{/feeds}}
</ul>
{{#pager}}
  <ul class="pager clearfix">
    {{#pages}}
      {{#current}}
        <li class="current"><a href="#{{href}}">{{ page }}</a></li>
      {{/current}}
      {{^current}}
      <li><a href="#{{href}}">{{ page }}</a></li>
      {{/current}}
    {{/pages}}
  </ul>
{{/pager}}
