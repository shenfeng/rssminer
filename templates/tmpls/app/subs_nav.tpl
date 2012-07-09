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
        <li class="item ficon-error"
          id="item-{{id}}" data-id="{{id}}" title="{{title}}">
          <a href="#{{href}}">
            <img src="{{img}}" width="16" height="16" />
            <i class="icon-rss"></i>
            <span class="title"> {{{title}}} </span>
            <span class="count">
              {{#dislike}}
                <span class="unread-dislike"
                  title="dislike count">{{dislike}}</span>
              {{/dislike}}
              {{#neutral}}
                <span class="unread-neutral"
                  title="neutral count">{{neutral}}</span>
              {{/neutral}}
              {{#like}}
                <span class="unread-like" title="like count">{{like}}</span>
              {{/like}}
            </span>
          </a>
        </li>
      {{/subs}}
    </ul>
  </li>
{{/groups}}
