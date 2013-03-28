<ul class="feeds">
  {{#feeds}}
    <li class="feed {{cls}}" data-id="{{id}}" id="feed-{{id}}">
      <a href="#{{href}}">
        <!-- <span class="indicator" data-title="{{i_tooltip}}"></span> -->
        <i class="icon-thumbs-up"
          data-title="{{m_like_title}}">
        </i>
        <span class="title" data-title="{{title_booltip}}">{{{title}}}</span>
        <!-- <i class="thumbs"> -->
        <!--   <i class="icon-thumbs-up" -->
        <!--     data-title="{{m_like_title}}"> -->
        <!--   </i> -->
        <!--   <i class="icon-thumbs-down" -->
        <!--     data-title="{{m_dislike_title}}"> -->
        <!--   </i> -->
        <!-- </i> -->
        <span class="author" data-title="{{tooltip}}">{{author}}</span>
        <span class="date">{{ date }}</span>
      </a>
    </li>
  {{/feeds}}
  {{^feeds}}<h2>{{m_no_entries}}</h2>{{/feeds}}
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
