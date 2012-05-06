{{#subs}}
  <li {{#collapse}} class="collapse" {{/collapse}}>
    <div class="folder" data-name="{{group}}">
      <img src="/imgs/folder.png"/>
      <span>{{group}}</span>
    </div>
    <ul class="rss-category">
      {{#list}}
        <li class="item" id="item-{{id}}" data-id="{{id}}" title="{{title}}">
          <a href="#{{href}}">
            <img src="{{img}}" width="16" height="16" />
            <span class="title"> {{title}} </span>
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
      {{/list}}
    </ul>
  </li>
{{/subs}}
