<div class="section">
  <h3>server control</h3>
  <table>
    <tr>
      <td>Cralwer</td>
      <td>{{crawler_runing}} </td>
      <td>
        <button>
          {{#crawler_runing}}stop {{/crawler_runing}}
            {{^crawler_runing}}start {{/crawler_runing}}
        </button>
      </td>
    </tr>
    <tr>
      <td>Fetcher</td>
      <td>{{fetcher_runing}} </td>
      <td>
        <button>
          {{#fetcher_runing}}stop {{/fetcher_runing}}
            {{^fetcher_runing}}start {{/fetcher_runing}}
        </button>
      </td>
    </tr>
  </table>
</div>

<div class="section">
  <h3>statistics</h3>
  <table>
    <tr>
      <td> Feed: </td>
      <td>{{feeds_count}} </td>
    </tr>
    <tr>
      <td> Rss: </td>
      <td>{{rss_links_cout}} </td>
    </tr>
    <tr>
      <td> Links: </td>
      <td>{{crawler_links_count}}</td>
    </tr>
    <tr>
      <td> Crawled: </td>
      <td>{{crawled_count}}</td>
    </tr>
    <tr>
      <td> Pending: </td>
      <td>{{pending_count}}</td>
    </tr>
  </table>
</div>

<div class="section">
  <h3>black domain</h3>
  <input id="add-patten"/>
  <ul>
    {{#black_domain_pattens}}
      <li>{{.}}</li>
    {{/black_domain_pattens}}
  </ul>
</div>

<div class="section">
  <h3>reseted domain</h3>
  <input/>
  <ul>
    {{#reseted_domain_pattens}}
      <li>{{.}}</li>
    {{/reseted_domain_pattens}}
  </ul>
</div>


