function isNumeric(n) {
    return !Number.isNaN(n) && Number.isFinite(n);
}

function binaryByteFormat(byteVal) {
    const isNegative = byteVal < 0;
    const bytes = Math.abs(byteVal);
    if (!isNumeric(bytes)) {
        return '--';
    }
    const sizes = ['B', 'KiB', 'MiB', 'GiB', 'TiB', 'PiB'];
    if (bytes === 0) {
        return '0';
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

function compactFormat(number) {
    const n = +number;
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