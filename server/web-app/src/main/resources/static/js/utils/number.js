function isNumeric(n) {
    return !Number.isNaN(n) && Number.isFinite(n);
}

function numberBasedOn(val, based) {
    return Math.floor(val / based) * based;
}

Number.prototype.basedOn = function (mod) {
    return numberBasedOn(this.valueOf(), mod);
}

Number.prototype.formatBinaryByte = function () {
    const byteVal = this.valueOf();
    const isNegative = byteVal < 0;
    const bytes = Math.abs(byteVal);
    if (!isNumeric(bytes)) {
        return '--';
    }
    const sizes = ['B', 'KiB', 'MiB', 'GiB', 'TiB', 'PiB'];
    if (bytes === 0) {
        return '0B';
    }
    const i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)), 10);
    let s;
    if (i <= 0) {
        s = `${Math.round(bytes * 100) / 100}${sizes[0]}`;
    } else {
        s = `${(bytes / (1024 ** i)).toFixed(2)}${sizes[i]}`;
    }
    return isNegative ? '-' + s : s;
}

Number.prototype.formatCompactNumber = function () {
    const n = +this.valueOf();
    if (!isNumeric(n)) {
        return '--';
    }

    const sizes = ['', 'K', 'M', 'G', 'T', 'P'];
    if (n === 0) {
        return '0';
    }
    const i = parseInt(Math.floor(Math.log(n) / Math.log(1000)), 10);
    if (i <= 0) {
        return `${Math.round(n * 100) / 100}`;
    }
    return `${Math.round((n / (1000 ** i)) * 100) / 100}${sizes[i]}`;
}

Number.prototype.formatWithNoTrailingZeros = function (fraction) {
    let n = this.valueOf().toFixed(fraction === undefined ? 2 : fraction);

    // remove trailing zeros to make the string compacted
    const dot = n.indexOf('.');
    if (dot !== -1) {
        let i = n.length - 1;
        for (; i >= dot; i--) {
            if (n.charAt(i) !== '0') {
                break;
            }
        }
        const endExclusiveIndex = (n.charAt(i) === '.') ? i : i + 1;
        return n.substring(0, endExclusiveIndex);
    }

    return n;
};

/**
 * @timestamp The diff between the given timestamp and current timestamp
 * @return e.g. 2 minutes ago
 */
// Number.prototype.formatTimeDiff(timestamp) {
//
// }

/**
 * value in milli-second
 * @returns {string}
 */
Number.prototype.formatTimeDuration = function() {
    const duration = +this.valueOf();

    let seconds = Math.floor(duration / 1000);

    const days = Math.floor(seconds / (24 * 3600));
    seconds = seconds % (24 * 3600); // get the left seconds for hours

    const hours = Math.floor(seconds / (3600));
    seconds = seconds % (3600);  // get the left seconds for minutes

    const minutes = Math.floor(seconds / (60));
    seconds = seconds % (60); // left seconds

    let text = '';
    if (days > 0)
        text += days + 'Day ';
    if (hours > 0)
        text += hours + 'Hour ';
    if (minutes > 0)
        text += minutes + 'Min';

    // no need to show seconds to make the text short
    if (text.length === 0 && seconds > 0)
        text += seconds + 'Second';

    return text;
}

String.prototype.htmlEncode = function () {
    return this.replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/'/g, '&#39;')
        .replace(/"/g, '&#34;')
        .replace(/\//, '&#x2F;');
}