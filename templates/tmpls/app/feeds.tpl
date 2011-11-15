<ul>
  {{#feeds}}
    <li data-id="{{id}}">
      <h4>{{title}}</h4>
      <span class="date">{{date}}</span>
      <div class="vote-host">
        <span class="vote">
          <span class="up"></span>
          <span class="down"></span>
        </span>
        <img class="favicon" width=16 height=16
             src="http://g.etfv.co/{{host}}"/>
        <span class="host">{{host}}</span>
      </div>
      <p class="snippet">{{snippet}}</p>
    </li>
  {{/feeds}}
</ul>
