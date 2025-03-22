
document.getElementById('type').addEventListener('change', function() {
    const type = this.value;
    document.getElementById('website-form').style.display = type === 'website' ? 'block' : 'none';
    document.getElementById('tel-form').style.display = type === 'tel' ? 'block' : 'none';
    document.getElementById('sms-form').style.display = type === 'sms' ? 'block' : 'none';
    document.getElementById('mail-form').style.display = type === 'mail' ? 'block' : 'none';
    document.getElementById('wifi-form').style.display = type === 'wifi' ? 'block' : 'none';
    document.getElementById('vcard-form').style.display = type === 'vcard' ? 'block' : 'none';
    document.getElementById('text-form').style.display = type === 'text' ? 'block' : 'none';
});

function generateQRCode() {
    const type = document.getElementById('type').value;
    let qrData = '';

    if (type === 'website') {
        const url = document.getElementById('url').value;
        qrData = url;
    } else if (type == 'tel') {
        const tel = document.getElementById('tel').value;
        qrData = `tel:${tel}`;
    } else if (type == 'sms') {
        const smsphone = document.getElementById('smsphone').value;
        const smsmessage = document.getElementById('smsmessage').value;
        qrData = `sms:${smsphone};?&body=${smsmessage}`
    } else if (type == 'mail') {
        const mail = document.getElementById('mail').value;
        qrData = `mailto:${mail}?subject=${subject}&body=${message}`
    } else if (type === 'wifi') {
        const ssid = document.getElementById('ssid').value;
        const password = document.getElementById('password').value;
        const encryption = document.getElementById('encryption').value;
        qrData = `WIFI:S:${ssid};T:${encryption};P:${password};;`;
    } else if (type === 'vcard') {
        const name = document.getElementById('name').value;
        const email = document.getElementById('email').value;
        const phone = document.getElementById('phone').value;
        qrData = `BEGIN:VCARD\nVERSION:3.0\nFN:${name}\nEMAIL:${email}\nTEL:${phone}\nEND:VCARD`;
    } else if (type === 'text') {
        const text = document.getElementById('text').value;
        qrData = text;
    }

    const qrCodeImg = document.getElementById('qr-code').querySelector('img');
    qrCodeImg.src = `/qr?text=${encodeURIComponent(qrData)}`;
}