<div class="lfloat" id="feeds">
  <ul>
    {{#feeds}}
      <li class="feed" data-id={{docId}}>
        <h3>{{title}}</h3>
        <div class="clearfix">
          <span class="author">
            by <a href="/browse/author:{{authorTag}}">{{author}}</a>
          </span>
          <ul class="tags rfloat">
            {{#categories}}
              <li><a href="/browse/tag:{{.}}">{{.}}</a></li>
            {{/categories}}
          </ul>
        </div>
        <p class="snippet">{{snippet}}</p>
      </li>
    {{/feeds}}
  </ul>
</div>

<div class="rfloat" id="right-side">
  <div id="tags">
    <p class="section-title">By Tag</p>
    <ul>
      {{#tags}}
        <li><a href="/browse/tag:{{.}}">{{.}}</a></li>
      {{/tags}}
    </ul>
  </div>
</div>

