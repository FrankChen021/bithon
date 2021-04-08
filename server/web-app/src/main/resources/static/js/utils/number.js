function isNumeric(n) {
    return !Number.isNaN(n) && Number.isFinite(n);
}

function binaryByteFormat(byteVal) {
    const bytes = +byteVal;
    if (!isNumeric(bytes)) {
        return '--';
    }
    const sizes = ['B', 'KiB', 'MiB', 'GiB', 'TiB', 'PiB'];
    if (bytes === 0) {
        return '0';
    }
    const i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)), 10);
    if (i <= 0) {
        return `${Math.round(bytes * 100) / 100}${sizes[0]}`;
    }
    return `${(bytes / (1024 ** i)).toFixed(2)}${sizes[i]}`;
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