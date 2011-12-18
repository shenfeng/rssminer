<div id="tabs">
  <ul id="controls">
    <li>Rss</li>
    <li>Orignal</li>
  </ul>
  <div class="tab">
    <h3>{{title}}</h3>
    <a href="{{link}}" target="_blank">
      <img src="/imgs/external-link.png"/>
    </a>

    <p class="author-time">
      by <span class="author">{{author}}</span>
      at <span class="time">{{ published_ts }}</span>
    </p>
    <div class="content">{{{summary}}}</div>
  </div>
  <div class="tab selected">
    <iframe src="{{link}}"></iframe>
  </div>
</div>

