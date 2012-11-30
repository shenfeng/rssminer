{{#groups}}
  <li {{#collapse}} class="collapse" {{/collapse}}>
    {{#group}}
      <a class="folder" data-name="{{name}}" href="#{{hash}}">
        <i class="icon-folder-open"></i>
        <i class="icon-folder-close"></i>
        <span>{{{name}}}</span>
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
              <span class="count">
                {{#dislike}}
                  <span class="unread-dislike"
                    data-title="{{m_unread_dislike}}">{{dislike}}</span>
                {{/dislike}}
                {{#neutral}}
                  <span class="unread-neutral"
                    data-title="{{m_neutral_count}}">{{neutral}}</span>
                {{/neutral}}
                {{#like}}
                  <span class="unread-like"
                    data-title="{{m_like_count}}">{{like}}</span>
                {{/like}}
              </span>
          </a>
        </li>
      {{/subs}}
    </ul>
  </li>
{{/groups}}
