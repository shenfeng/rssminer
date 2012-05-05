<div>
  <ul class="subs">
    {{#subs}}
      <li>
        <a href="#{{href}}">
          <img src="{{img}}" width="16" height="16" />
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
    {{/subs}}
  </ul>
</div>
