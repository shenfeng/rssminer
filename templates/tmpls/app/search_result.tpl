<div>
  <ul class="subs">
    {{#subs}}
      <li>
        <a href="#{{href}}">
          <img src="{{img}}" width="16" height="16" />
          <span class="title"> {{title}} </span>
          <span class="count">
            <span class="total" title="total feed">{{ total }}</span>
            {{#dislike}}
              <span class="unread-dislike"
                title="dislike count">{{dislike}}</span>
            {{/dislike}}
            {{#neutral}}
              <span class="unread-neutral"
                title="neutral count">{{neutral}}</span>
            {{/neutral}}
            {{#like}}
              <span class="unread-like"
                title="like count">{{like}}</span>
            {{/like}}
          </span>
        </a>
      </li>
    {{/subs}}
  </ul>
</div>
