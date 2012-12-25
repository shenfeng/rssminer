{{#groups}}
  <li {{#collapse}} class="collapse" {{/collapse}}>
    {{#group}}
      <a class="folder" data-name="{{name}}" href="#{{hash}}">
        <i class="icon-folder-open"></i>
        <i class="icon-folder-close"></i>
        <span>{{{name}}}</span>
        <div class="icon-caret-down" data-title="{{m_item_option}}"></div>
      </a>
    {{/group}}
    <ul class="rss-category">
      {{#subs}}
        <li class="item ficon-error {{cls}}" id="item-{{id}}"
          data-id="{{id}}" data-title="{{tooltip}}">
          <a href="#{{href}}">
            {{#img}}<img src="{{img}}" width="16" height="16" />{{/img}}
              <i class="icon-rss"></i>
              <span class="title"> {{{title}}} </span>
              {{#unread}}
                <span class="c"> ({{ unread }}) </span>
              {{/unread}}
              <i class="icon-caret-down" data-title="{{m_item_option}}"></i>
          </a>
        </li>
      {{/subs}}
    </ul>
  </li>
{{/groups}}
