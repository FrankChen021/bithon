class AutoRefresher {

    constructor(option) {
        this._listeners = [];
        this._timerLength = option.timerLength == null ? 30 : option.timerLength;
        this._countDown = this._timerLength;
        this._timer = null;

        this._control = $(
        '<div class="dropdown">                                                                                                                                          ' +
        '    <button class="btn btn-default dropdown-toggle" type="button" id="refreshIntervalButton" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false"> ' +
        '        Auto Refresh&nbsp;<span>(DISABLED)</span>                                                                                         ' +
        '    </button>                                                                                                                                                   ' +
        '    <div class="dropdown-menu" aria-labelledby="refreshIntervalButton">                                                                                         ' +
        '        <a class="dropdown-item" data-text="(DISABLED)" href="#">Disable</a>                                                                                                            ' +
        '        <a class="dropdown-item" data-text="10" href="#">10s</a>                                                                                                            ' +
        '        <a class="dropdown-item" data-text="30" href="#">30s</a>                                                                                                    ' +
        '        <a class="dropdown-item" data-text="60" href="#">60s</a>                                                                                               ' +
        '    </div>                                                                                                                                                      ' +
        '</div>                                                                                                                                                          ');
        var btn  = this._control.find('input');
        $(btn).click((clickEvent)=>{
            var checked = clickEvent.target.value;
            if ( checked == 'on' ) {
                this.start();
            } else {
                this.stop();
            }
        });
        this._control.find('a').click((clickEvent)=>{
            var text = $(clickEvent.target).attr('data-text');

            $(this._countDownText).text(text);
            if ( text != '(DISABLED)') {
                // restart timer
                this._timerLength = parseInt(text);
                this.start();
            } else {
                this.stop();
            }
        });
        this._countDownText = this._control.find('span');
    }

    // PUBLIC
    start() {
        this.stop();
        if ( this._timerLength == NaN ) {
            return;
        }
        this._timer = window.setInterval(()=>{
            --this._countDown;

            //update UI
            $(this._countDownText).text(this._countDown < 10 ? '0' + this._countDown : this._countDown);

            //fire event
            if ( this._countDown === 0 ) {

                $.each(this._listeners, (index, listener)=>{
                    try {
                        listener.apply();
                    } catch(e) {
                        console.log(e);
                    }
                });

                this._countDown = this._timerLength;
            }
        }, 1000);
    }

    stop() {
        if ( this._timer != null ) {
            window.clearInterval(this._timer);
            this._timer = null;
        }
        this._countDown = this._timerLength;
    }

    childOf(element) {
        element.append(this._control);
        return this;
    }

    registerRefreshListener(listener) {
        this._listeners.push(listener);
    }
}