<div id="content">
  <ul id="entries">
    {{#feeds}}
    <li class="entry">
      <div class="collapsed">
        <div class="entry-main">
          <h4><a href="{{link}}" target="_blank">{{title}}</a></h4> -
          <span class="snippet">{{snippet}}</span>
        </div>
      </div>
      <div class="entry-container">
        <div class="entry-main">
          <h2 class="entry-title">{{title}}</h2>
          <span class="entry-author">{{author}}</span>
          <div class="entry-body">
            {{{summary}}}
          </div>
        </div>
        <div class="entry-comments">
        </div>
      </div>
    </li>
    {{/feeds}}
  </ul>
</div>
