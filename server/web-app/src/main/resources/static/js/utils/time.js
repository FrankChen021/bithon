function formatDateTime(dateTime, fmt) { //author: meizz
    const o = {
        "M+": dateTime.getMonth() + 1, //月份
        "d+": dateTime.getDate(), //日
        "h+": dateTime.getHours(), //小时
        "m+": dateTime.getMinutes(), //分
        "s+": dateTime.getSeconds(), //秒
        "q+": Math.floor((dateTime.getMonth() + 3) / 3), //季度
        "S": dateTime.getMilliseconds().toString().padStart(3, "0") //毫秒
    };
    if (/(y+)/.test(fmt)) {
        fmt = fmt.replace(RegExp.$1, (dateTime.getFullYear() + "").substr(4 - RegExp.$1.length));
    }
    for (const k in o)
        if (new RegExp("(" + k + ")").test(fmt)) {
            fmt = fmt.replace(RegExp.$1, (RegExp.$1.length === 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
        }
    return fmt;
}

Date.prototype.format = function (fmt) {
    return formatDateTime(this, fmt);
}

Date.prototype.truncate = function (base) {
    this.setTime(Math.floor(this.getTime() / base)*base);
    return this;
}

function nanoFormat(nanoTime, fractionDigits) {
    return timeFormat(nanoTime, fractionDigits, ['ns', 'us', 'ms', 's']);
}

function microFormat(milliTime, fractionDigits) {
    return timeFormat(milliTime, fractionDigits, ['us', 'ms', 's'])
}

function milliFormat(milliTime, fractionDigits) {
    return timeFormat(milliTime, fractionDigits, ['ms', 's'])
}

function timeFormat(time, fractionDigits, units) {
    let val = +time || 0;
    let index = 0;
    if (val <= 0) return '0';
    while (val >= 1000 && index < units.length - 1) {
        index += 1;
        val = time / (1000 ** index);
    }

    // let n = val.toFixed(fractionDigits === undefined ? 2 : fractionDigits);
    //
    // // remove trailing zeros to make the string compacted
    // const dot = n.indexOf('.');
    // if (dot !== -1) {
    //     let i = n.length - 1;
    //     for (; i >= dot; i--) {
    //         if (n.charAt(i) !== '0') {
    //             break;
    //         }
    //     }
    //     const endExclusiveIndex = (n.charAt(i) === '.') ? i : i + 1;
    //     n = n.substring(0, endExclusiveIndex) + units[index];
    //     //console.log(`${time} = ${n}`);
    //     return n;
    // }

    return val.formatWithNoTrailingZeros(fractionDigits) + units[index];
}

Date.prototype.diff = function (before) {
    const now = this.getTime();

    const seconds = Math.floor((now - before) / 1000);
    if (seconds < 60) {
        return seconds + " seconds ago";
    }
    const minute = Math.floor(seconds / 60);
    if (minute < 60) {
        return minute + " minutes ago";
    }
    const hours = Math.floor(minute / 60);
    if (hours < 24) {
        return hours + " hours ago";
    }
    const day = Math.floor(hours / 24);
    if (day < 365) {
        return day + " days ago";
    }
    const year = Math.floor(day / 365);
    return year + " years ago";
}
