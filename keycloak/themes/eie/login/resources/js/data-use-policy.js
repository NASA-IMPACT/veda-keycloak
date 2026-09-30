(function () {
  var back = document.querySelector('[data-policy-back]');
  if (!back) return;
  back.addEventListener('click', function () {
    window.history.back();
  });
})();
