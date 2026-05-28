(function () {
  function digits(value) {
    return (value || "").replace(/\D/g, "");
  }

  function limit(value, size) {
    return digits(value).slice(0, size);
  }

  function maskCpf(value) {
    const v = limit(value, 11);
    return v
      .replace(/^(\d{3})(\d)/, "$1.$2")
      .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
      .replace(/\.(\d{3})(\d)/, ".$1-$2");
  }

  function maskPhone(value) {
    const v = limit(value, 11);
    return v
      .replace(/^(\d{2})(\d)/, "($1)$2")
      .replace(/(\d{5})(\d)/, "$1-$2");
  }

  function maskCep(value) {
    const v = limit(value, 8);
    return v.replace(/^(\d{5})(\d)/, "$1-$2");
  }

  function maskDate(value) {
    const v = limit(value, 8);
    return v
      .replace(/^(\d{2})(\d)/, "$1/$2")
      .replace(/^(\d{2})\/(\d{2})(\d)/, "$1/$2/$3");
  }

  function applyMask(input) {
    const type = input.dataset.mask;
    if (type === "cpf") input.value = maskCpf(input.value);
    if (type === "phone") input.value = maskPhone(input.value);
    if (type === "cep") input.value = maskCep(input.value);
    if (type === "date") input.value = maskDate(input.value);
  }

  function setupCepLookup(input) {
    input.addEventListener("blur", function () {
      const cep = digits(input.value);
      if (cep.length !== 8) return;

      fetch("https://viacep.com.br/ws/" + cep + "/json/")
        .then(function (response) { return response.ok ? response.json() : null; })
        .then(function (data) {
          if (!data || data.erro) return;
          const form = input.form;
          if (!form) return;

          const logradouro = form.querySelector("[data-cep-logradouro]");
          const bairro = form.querySelector("[data-cep-bairro]");
          const municipio = form.querySelector("[name='municipio']");
          const uf = form.querySelector("[name='uf']");

          if (logradouro && data.logradouro) logradouro.value = data.logradouro;
          if (bairro && data.bairro) bairro.value = data.bairro;
          if (municipio && data.localidade) municipio.value = data.localidade;
          if (uf && data.uf) uf.value = data.uf;
        })
        .catch(function () {});
    });
  }

  function setupValidation(form) {
    form.addEventListener("submit", function (event) {
      let valid = true;
      form.querySelectorAll("[data-digits]").forEach(function (input) {
        const expected = Number(input.dataset.digits);
        const value = digits(input.value);
        if (input.value && value.length !== expected) {
          input.setCustomValidity("Informe exatamente " + expected + " digitos.");
          valid = false;
        } else {
          input.setCustomValidity("");
          input.value = value;
        }
      });

      if (!valid) {
        event.preventDefault();
        form.reportValidity();
      }
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("[data-mask]").forEach(function (input) {
      applyMask(input);
      input.addEventListener("input", function () {
        applyMask(input);
        input.setCustomValidity("");
      });
    });

    document.querySelectorAll("[data-cep-lookup]").forEach(setupCepLookup);
    document.querySelectorAll(".sigrec-validated-form").forEach(setupValidation);
  });
})();
