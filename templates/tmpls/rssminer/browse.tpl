<div class="lfloat" id="feeds">
  <ul>
    {{#feeds}}
      <li>
        <h3>{{title}}</h3>
        <div class="clearfix">
          <span class="author">
            by <a href="/browse/author:{{author}}">{{author}}</a>
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

<div class="rfloat" id="tags">
  <p class="section-title">By Tag</p>
  <ul id="tags">
    {{#tags}}
      <li><a href="/browse/tag:{{.}}">{{.}}</a></li>
    {{/tags}}
  </ul>
</div>

