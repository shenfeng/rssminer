<h2>
  {{#url}}
    <a href="{{url}}" target="_blank">{{ title }}</a>
  {{/url}}
  {{^url}}{{ title }}{{/url}}
</h2>
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
        <span class="title">{{{title}}}</span>
        <i class="thumbs">
          <i class="icon-thumbs-up"
            data-title="like it, give me more like this in recommend tab">
          </i>
          <i class="icon-thumbs-down"
            data-title="dislike, less in recommend tab">
          </i>
        </i>
        <span class="author"
          data-title="author">{{author}}</span>
        <span class="date">{{ date }}</span>
      </a>
    </li>
  {{/feeds}}
  {{^feeds}}<h2>No entries</h2>{{/feeds}}
</ul>
{{#pager}}
  <ul class="pager">
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
