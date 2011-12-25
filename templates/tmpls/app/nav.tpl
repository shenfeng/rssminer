{{#subs}}
  <li>
    <a href="">
      <div class="folder">
        <img src="/imgs/folder.png"/>
        <span>{{tag}}</span>
      </div>
    </a>
    <ul>
      {{#list}}
        <li class="item" id="item-{{id}}">
          <a href="#{{href}}">
            <img src="{{img}}" width="16" onerror="RM.iconError(this)"
            height="16" />
            <span class="title"> {{title}} </span>
            <span class="count">
              {{#dislike}}
                <span class="unread-dislike">{{dislike}}</span>
              {{/dislike}}
              {{#neutral}}
                <span class="unread-neutral">{{neutral}}</span>
              {{/neutral}}
              {{#like}}
                <span class="unread-like">{{like}}</span>
              {{/like}}
            </span>
          </a>
        </li>
      {{/list}}
    </ul>
  </li>
{{/subs}}
