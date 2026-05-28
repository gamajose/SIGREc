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

  function isValidCpf(value) {
    const cpf = digits(value);
    if (cpf.length !== 11) return false;
    if (/^(\d)\1{10}$/.test(cpf)) return false;

    let sum = 0;
    for (let i = 0; i < 9; i++) sum += Number(cpf.charAt(i)) * (10 - i);
    let check = 11 - (sum % 11);
    if (check >= 10) check = 0;
    if (check !== Number(cpf.charAt(9))) return false;

    sum = 0;
    for (let i = 0; i < 10; i++) sum += Number(cpf.charAt(i)) * (11 - i);
    check = 11 - (sum % 11);
    if (check >= 10) check = 0;
    return check === Number(cpf.charAt(10));
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
          const municipio = form.querySelector("[data-cep-municipio], [name='municipio']");
          const uf = form.querySelector("[data-cep-uf], [name='uf']");

          if (logradouro && data.logradouro) logradouro.value = data.logradouro;
          if (bairro && data.bairro) bairro.value = data.bairro;
          if (municipio && data.localidade) municipio.value = data.localidade;
          if (uf && data.uf) uf.value = data.uf;
        })
        .catch(function () {});
    });
  }

  function setupUnidadeCnesLookup(input) {
    input.addEventListener("blur", function () {
      const cnes = digits(input.value);
      if (cnes.length !== 7) return;
      const form = input.form;
      if (!form) return;

      fetch("/api/unidades/cnes/" + cnes, { cache: "no-store" })
        .then(function (response) { return response.ok ? response.json() : null; })
        .then(function (data) {
          if (!data) return;
          const nome = form.querySelector("[name='nome']");
          const municipio = form.querySelector("[name='municipio']");
          const uf = form.querySelector("[name='uf']");
          const telefone = form.querySelector("[name='telefone']");
          const responsavel = form.querySelector("[name='responsavel']");

          if (nome && data.nome) nome.value = data.nome;
          if (municipio && data.municipio) municipio.value = data.municipio;
          if (uf && data.uf) uf.value = data.uf;
          if (telefone && data.telefone) telefone.value = maskPhone(data.telefone);
          if (responsavel && data.responsavel) responsavel.value = data.responsavel;
        })
        .catch(function () {});
    });
  }

  function setupProfissionalConselhoLookup(input) {
    input.addEventListener("blur", function () {
      const registro = digits(input.value);
      if (!registro) return;
      const form = input.form;
      if (!form) return;

      const conselhoInput = form.querySelector("[name='conselho']");
      const ufInput = form.querySelector("[name='uf_conselho']");
      const conselho = conselhoInput ? conselhoInput.value.trim() : "";
      const uf = ufInput ? ufInput.value.trim() : "";
      if (!conselho) return;

      const url = "/api/profissionais/conselho?conselho=" + encodeURIComponent(conselho)
        + "&registro=" + encodeURIComponent(registro)
        + (uf ? "&uf=" + encodeURIComponent(uf) : "");

      fetch(url, { cache: "no-store" })
        .then(function (response) { return response.ok ? response.json() : null; })
        .then(function (data) {
          if (!data) return;
          const nome = form.querySelector("[name='nome']");
          const cpfCns = form.querySelector("[name='cpf_cns']");
          const especialidade = form.querySelector("[name='especialidade']");

          if (nome && data.nome) nome.value = data.nome;
          if (cpfCns && data.cpf_cns) cpfCns.value = data.cpf_cns;
          if (especialidade && data.especialidade) especialidade.value = data.especialidade;
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
        } else if (input.dataset.mask === "cpf" && value && !isValidCpf(value)) {
          input.setCustomValidity("Informe um CPF valido.");
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
    document.querySelectorAll("[data-cnes-lookup]").forEach(setupUnidadeCnesLookup);
    document.querySelectorAll("[data-conselho-lookup]").forEach(setupProfissionalConselhoLookup);
    document.querySelectorAll(".sigrec-validated-form").forEach(setupValidation);
  });
})();