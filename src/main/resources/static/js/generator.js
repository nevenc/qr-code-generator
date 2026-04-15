
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

const includeLogoCheckbox = document.getElementById('include-logo');
const logoFileInput = document.getElementById('logo-file');
const logoError = document.getElementById('logo-error');

includeLogoCheckbox.addEventListener('change', clearLogoError);
logoFileInput.addEventListener('change', clearLogoError);

function showLogoError(message) {
    logoError.textContent = message;
    logoError.hidden = false;
}

function clearLogoError() {
    logoError.textContent = '';
    logoError.hidden = true;
}

function buildQrData() {
    const type = document.getElementById('type').value;
    if (type === 'website') {
        return document.getElementById('url').value;
    } else if (type === 'tel') {
        return `tel:${document.getElementById('tel').value}`;
    } else if (type === 'sms') {
        const smsphone = document.getElementById('smsphone').value;
        const smsmessage = document.getElementById('smsmessage').value;
        return `sms:${smsphone};?&body=${smsmessage}`;
    } else if (type === 'mail') {
        const mail = document.getElementById('mail').value;
        const subject = document.getElementById('subject').value;
        const message = document.getElementById('message').value;
        return `mailto:${mail}?subject=${subject}&body=${message}`;
    } else if (type === 'wifi') {
        const ssid = document.getElementById('ssid').value;
        const password = document.getElementById('password').value;
        const encryption = document.getElementById('encryption').value;
        return `WIFI:S:${ssid};T:${encryption};P:${password};;`;
    } else if (type === 'vcard') {
        const name = document.getElementById('name').value;
        const email = document.getElementById('email').value;
        const phone = document.getElementById('phone').value;
        return `BEGIN:VCARD\nVERSION:3.0\nFN:${name}\nEMAIL:${email}\nTEL:${phone}\nEND:VCARD`;
    } else if (type === 'text') {
        return document.getElementById('text').value;
    }
    return '';
}

async function generateQRCode() {
    clearLogoError();
    const qrData = buildQrData();

    const form = new FormData();
    form.append('text', qrData);
    form.append('includeLogo', includeLogoCheckbox.checked ? 'true' : 'false');
    if (includeLogoCheckbox.checked && logoFileInput.files[0]) {
        form.append('logo', logoFileInput.files[0]);
    }

    try {
        const resp = await fetch('/qr', { method: 'POST', body: form });
        if (!resp.ok) {
            const msg = await resp.text();
            showLogoError(msg || `Error: ${resp.status}`);
            return;
        }
        const blob = await resp.blob();
        const qrCodeImg = document.getElementById('qr-code').querySelector('img');
        qrCodeImg.src = URL.createObjectURL(blob);
    } catch (e) {
        showLogoError(`Network error: ${e.message}`);
    }
}
