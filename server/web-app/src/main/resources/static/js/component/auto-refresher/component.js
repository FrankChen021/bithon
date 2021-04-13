class AutoRefresher {

    constructor(option) {
        this._listeners = [];
        this._timerLength = option.timerLength == null ? 30 : option.timerLength;
        this._countDown = this._timerLength;
        this._timer = null;

        this._control = $(
            '<div class="input-group">' +
            '   <div class="input-group-prepend">' +
            '       <button class="btn btn-outline-secondary" type="button" id="manualRefresh"><i class="fas fa-sync-alt"></i></button>' +
            '       <button class="btn btn-outline-secondary dropdown-toggle dropdown-toggle-split" type="button" id="autoRefresh" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false"> ' +
            '           <span></span>' +
            '       </button>' +
            '       <div class="dropdown-menu" aria-labelledby="autoRefresh">' +
            '           <a class="dropdown-item" data-text="(DISABLED)" href="#">Disable</a>' +
            '           <a class="dropdown-item" data-text="10" href="#">10s</a>' +
            '           <a class="dropdown-item" data-text="30" href="#">30s</a>' +
            '           <a class="dropdown-item" data-text="60" href="#">60s</a>' +
            '       </div>' +
            '    </div>' +
            '</div>');

        this._control.find('a').click((clickEvent) => {
            // click auto refresh items
            const text = $(clickEvent.target).attr('data-text');
            if (text !== '(DISABLED)') {
                $(this._countDownText).text(text);

                // restart timer
                this._timerLength = parseInt(text);
                this.start();
            } else {
                $(this._countDownText).text('');

                this.stop();
            }
        });

        this._control.find('#manualRefresh').click((clickEvent) => {
            // re-start time
            if ( this.isAutoRefreshEnabled() ) {
                this.start();
            }

            this.fireRefreshEvent();
        });

        this._countDownText = this._control.find('span');
    }

    // PUBLIC
    start() {
        this.stop();

        if (isNaN(this._timerLength)) {
            return;
        }

        this._timer = window.setInterval(() => {
            --this._countDown;

            //update UI
            $(this._countDownText).text(this._countDown < 10 ? '0' + this._countDown : this._countDown);

            //fire event
            if (this._countDown === 0) {

                this.fireRefreshEvent();

                this._countDown = this._timerLength;
            }
        }, 1000);
    }

    fireRefreshEvent() {
        $.each(this._listeners, (index, listener) => {
            try {
                listener.apply();
            } catch (e) {
                console.log(e);
            }
        });
    }

    stop() {
        if (this._timer != null) {
            window.clearInterval(this._timer);
            this._timer = null;
        }
        this._countDown = this._timerLength;
    }

    isAutoRefreshEnabled() {
        return this._timer != null;
    }

    childOf(element) {
        element.append(this._control);
        return this;
    }

    registerRefreshListener(listener) {
        this._listeners.push(listener);
    }
}