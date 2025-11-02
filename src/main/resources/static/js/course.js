
const lessonBlocks = [];

function generateBlockHTML(block) {
   const attr = `data-learning="${block.learning || '0'}"`; // <— TU
  switch (block.type) {
    case "header":
      return `
      <div ${attr}>
        <h3 class="course-section-title text-start mt-0 mb-3">
          ${block.content}
        </h3>
        </div>
      `;
    case "text":
      return `
          <div class="course-text-content" ${attr}>
            <p>
              ${block.content}
            </p>
          </div>
      `;
    case "image":
      return `
        <div class="course-image-container my-4" ${attr}>
          <img
            src="${block.src}"
            alt="${block.alt}"
            class="course-image"
          />
        </div>
      `;
    case "video":
      return `
        <div class="course-video-container my-4" ${attr}>
          <iframe
            src="${block.src}"
            title="${block.title}"
            frameborder="0"
            allowfullscreen
          ></iframe>
        </div>
      `;
    case "code":
      return `
    <div class="course-code-block my-4" ${attr}>
      <div class="course-code-header d-flex justify-content-between align-items-center mb-2">
        <span><i class="bi bi-code-slash me-2"></i>${block.title}</span>
        <button class="btn btn-sm btn-outline-light course-copy-btn" onclick="copyCode(this)">
          <i class="bi bi-clipboard"></i> Kopiuj
        </button>
      </div>
      <pre><code class="language-${block.language}">${escapeHtml(
        block.code
      )}</code></pre>
    </div>
  `;
    case "quiz":
      return `
    <div class="course-quiz-block my-4" ${attr}>
      <h4 class="course-quiz-question mb-3">
        <i class="bi bi-pencil-square me-2"></i>${block.question}
      </h4>

      <div class="course-quiz-input d-flex align-items-center flex-wrap gap-2">
        <input
          type="text"
          class="course-answer-input form-control"
          placeholder="Wpisz swoją odpowiedź..."
          data-answer="${block.answer}"
        />
        <button class="btn btn-gradient btn-sm check-answer-btn">Sprawdź</button>
      </div>

      <div class="course-quiz-feedback mt-3"></div>
    </div>
  `;
    case "quiz-choice":
      const optionsHTML = block.options
        .map(
          (opt, i) => `
      <button class="course-quiz-option" data-correct="${
        i === block.correctIndex
      }">${opt}</button>`
        )
        .join("");

      return `
    <div class="course-quiz-block my-4" ${attr}>
      <h4 class="course-quiz-question mb-3">
        <i class="bi bi-question-circle me-2"></i>${block.question}
      </h4>
      <div class="course-quiz-options">
        ${optionsHTML}
      </div>
      <div class="course-quiz-feedback mt-3"></div>
    </div>
  `;
    case "practice":
      const codeWithInput = block.code.replace(
        "[___]",
        `<input type='text' class='practice-input' placeholder='...' data-answer='${block.answer}'/>`
      );

      return `
    <div class="course-practice-block my-4" ${attr}>
      <h4 class="course-section-title mb-1">
        <i class="bi bi-terminal me-2"></i>${block.title}
      </h4>
      <pre><code class="language-${block.language}">${codeWithInput}</code></pre>
      <button class="btn btn-gradient btn-sm check-practice-btn mt-2">Sprawdź</button>
      <div class="course-quiz-feedback mt-2"></div>
    </div>
  `;
    case "note":
      return `
    <div class="course-note my-4" ${attr}>
      <i class="bi bi-journal-text me-2"></i>
      <strong>Notatka:</strong> ${block.content}
    </div>
  `;
    case "tip":
      return `
    <div class="course-text-block tip mt-3 mb-3" ${attr}>
      <h5 class="course-text-title">
        <i class="bi bi-lightbulb me-2"></i>Wskazówka
      </h5>
      <div class="course-text-content">
        <p><strong>Pamiętaj:</strong> ${block.content}</p>
      </div>
    </div>
  `;
    case "idea":
      return `
    <div class="course-idea my-4" ${attr}>
      <i class="bi bi-lightbulb me-2"></i>
      <strong>Pomysł:</strong> ${block.content}
    </div>
  `;
    case "warning":
      return `
    <div class="course-warning my-4" ${attr}>
      <i class="bi bi-exclamation-triangle me-2 text-warning"></i>
      <strong>Uwaga:</strong> ${block.content}
    </div>
  `;
    case "fact":
      return `
    <div class="course-fact my-4" ${attr}>
      <i class="bi bi-info-circle me-2 text-info"></i>
      <strong>Ciekawostka:</strong> ${block.content}
    </div>
  `;
    case "audio":
      return `
    <div class="course-audio-container my-4" ${attr}>
      <audio controls>
        <source src="${block.src}" type="audio/mpeg" />
        Twoja przeglądarka nie obsługuje elementu audio.
      </audio>
    </div>
  `;

    default:
      return "";
  }
}

function updateLessonPreview() {
  const previewContainer = document.getElementById("lessonPreview");
  previewContainer.innerHTML = lessonBlocks.map(generateBlockHTML).join("");
}

document.getElementById("addHeaderBtn").addEventListener("click", (e) => {
  const input = document.getElementById("headerText");
  const value = input.value.trim();
  const modal = e.target.closest(".modal");

  if (!value) {
    input.classList.add("is-invalid");
    return;
  }

  const selected = [...modal.querySelectorAll(".learning-type:checked")]
    .map(cb => cb.value)
    .join(",") || "0";


  const newBlock = {
    id: Date.now(),
    type: "header",
    content: value,
    learning: selected,
  };


  lessonBlocks.push(newBlock);


  updateLessonPreview();


  input.value = "";
  input.classList.remove("is-invalid");
  modal.querySelectorAll(".learning-type:checked").forEach(cb => cb.checked = false);

  const bsModal = bootstrap.Modal.getInstance(modal);
  bsModal.hide();

  console.log("Aktualne bloki lekcji:", lessonBlocks);
});



document.getElementById("addTextBtn").addEventListener("click", (e) => {
  const textarea = document.getElementById("lessonText");
  const value = textarea.value.trim();
  const modal = e.target.closest(".modal"); // pobieramy modal, w którym kliknięto

  if (!value) {
    textarea.classList.add("is-invalid");
    return;
  }


  const selected = [...modal.querySelectorAll(".learning-type:checked")]
    .map(cb => cb.value)
    .join(",") || "0"; // jeśli nic nie zaznaczone → 0 = wszyscy


  const newBlock = {
    id: Date.now(),
    type: "text",
    content: value,
    learning: selected, // 🧠 dodane
  };


  lessonBlocks.push(newBlock);
  updateLessonPreview();

  textarea.value = "";
  textarea.classList.remove("is-invalid");
  modal.querySelectorAll(".learning-type:checked").forEach(cb => cb.checked = false);

  const bsModal = bootstrap.Modal.getInstance(modal);
  bsModal.hide();

  console.log("Aktualne bloki lekcji:", lessonBlocks);
});


const pendingFiles = [];

const PLACEHOLDER_IMAGE = "../../static/img/unnamed.png";
const UPLOAD_BASE = "sciezka";


function slugifyLessonName() {
  const raw = document.getElementById("lessonName")?.value || "lesson";
  return raw
    .trim()
    .toLowerCase()
    .replace(/\s+/g, "_")
    .replace(/[^\w\-]+/g, "");
}


const pendingImageFiles = [];


document.getElementById("imageFile").addEventListener("change", (e) => {
  const file = e.target.files[0];
  const previewContainer = document.getElementById("imagePreview");
  const previewImg = document.getElementById("imagePreviewImg");

  previewContainer.classList.add("d-none");

  if (!file) return;

  const ext = file.name.split(".").pop().toLowerCase();
  if (!["jpg", "jpeg", "png"].includes(ext)) {
    alert("Dozwolone są tylko pliki JPG lub PNG!");
    e.target.value = "";
    return;
  }

  const url = URL.createObjectURL(file);
  previewImg.src = url;
  previewContainer.classList.remove("d-none");
});


document.getElementById("addImageBtn").addEventListener("click", (e) => {
    const modal = e.target.closest(".modal");
    const fileInput = document.getElementById("imageFile");
    const file = fileInput.files[0];

    if (!file) {
        alert("Wybierz plik JPG lub PNG!");
        return;
    }

    const ext = file.name.split(".").pop().toLowerCase();
    if (!["jpg", "jpeg", "png"].includes(ext)) {
        alert("Dozwolone są tylko pliki JPG lub PNG!");
        return;
    }

    const uniqueName = `${crypto.randomUUID()}-${file.name}`;


    Object.defineProperty(file, "name", { value: uniqueName });

    const previewUrl = URL.createObjectURL(file);

    const selected = [...modal.querySelectorAll(".learning-type:checked")]
        .map(cb => cb.value)
        .join(",") || "0";

    const newBlock = {
        id: Date.now(),
        type: "image",
        src: previewUrl,
        title: uniqueName,
        alt: file.name.replace(/\.[^.]+$/, ''),
        tempFileIndex: pendingImageFiles.length,
        learning: selected,
    };

    pendingImageFiles.push(file);
    lessonBlocks.push(newBlock);

    updateLessonPreview();

    fileInput.value = "";
    modal.querySelectorAll(".learning-type:checked").forEach(cb => cb.checked = false);
    document.getElementById("imagePreview").classList.add("d-none");
    const bsModal = bootstrap.Modal.getInstance(modal);
    bsModal.hide();

    console.log("🖼️ Dodano obraz:", uniqueName, "(", file.size, "B )");
});




function updateLessonPreview() {
  const previewContainer = document.getElementById("lessonPreview");
  if (!previewContainer) return;
  previewContainer.innerHTML = lessonBlocks.map(generateBlockHTML).join("");
}

document.getElementById("addVideoBtn").addEventListener("click", (e) => {
  const modal = e.target.closest(".modal"); // pobieramy modal
  const linkInput = document.getElementById("videoLink");
  const titleInput = document.getElementById("videoTitle");

  const link = linkInput.value.trim();
  const title = titleInput.value.trim() || "Przykładowe wideo";

  if (!link) {
    alert("Podaj link do filmu YouTube!");
    return;
  }

  const selected = [...modal.querySelectorAll(".learning-type:checked")]
    .map(cb => cb.value)
    .join(",") || "0"; // 0 = wszyscy


  const embedLink = link.replace("watch?v=", "embed/");


  const newBlock = {
    id: Date.now(),
    type: "video",
    src: embedLink,
    title: title,
    learning: selected,
  };

  lessonBlocks.push(newBlock);
  updateLessonPreview();


  linkInput.value = "";
  titleInput.value = "";
  modal.querySelectorAll(".learning-type:checked").forEach(cb => cb.checked = false);
  const bsModal = bootstrap.Modal.getInstance(modal);
  bsModal.hide();

  console.log("Bloki lekcji:", lessonBlocks);
});


function escapeHtml(text) {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

document.getElementById("addQuestionBtn").addEventListener("click", (e) => {
  const modal = e.target.closest(".modal");
  const question = document.getElementById("quizQuestion").value.trim();
  const answer = document.getElementById("quizAnswer").value.trim();

  if (!question || !answer) {
    alert("Wpisz pytanie i poprawną odpowiedź!");
    return;
  }

  const selected = [...modal.querySelectorAll(".learning-type:checked")]
    .map(cb => cb.value)
    .join(",") || "0";

  const newBlock = {
    id: Date.now(),
    type: "quiz",
    question,
    answer,
    learning: selected,
  };

  lessonBlocks.push(newBlock);
  updateLessonPreview();

  document.getElementById("quizQuestion").value = "";
  document.getElementById("quizAnswer").value = "";
  modal.querySelectorAll(".learning-type:checked").forEach(cb => cb.checked = false);
  const bsModal = bootstrap.Modal.getInstance(modal);
  bsModal.hide();
});


document.getElementById("addQuizBtn").addEventListener("click", (e) => {
  const modal = e.target.closest(".modal");
  const question = document.getElementById("quizMultiQuestion").value.trim();
  const options = Array.from(document.querySelectorAll(".quiz-option-input"))
    .map((i) => i.value.trim())
    .filter((v) => v !== "");
  const correctIndex = parseInt(document.getElementById("quizCorrectIndex").value) - 1;

  if (!question || options.length < 2) {
    alert("Podaj pytanie i co najmniej dwie odpowiedzi!");
    return;
  }

  if (correctIndex < 0 || correctIndex >= options.length) {
    alert("Numer poprawnej odpowiedzi musi mieścić się w zakresie 1–" + options.length);
    return;
  }

  const selected = [...modal.querySelectorAll(".learning-type:checked")]
    .map(cb => cb.value)
    .join(",") || "0";

  const newBlock = {
    id: Date.now(),
    type: "quiz-choice",
    question,
    options,
    correctIndex,
    learning: selected,
  };

  lessonBlocks.push(newBlock);
  updateLessonPreview();

  document.getElementById("quizMultiQuestion").value = "";
  document.querySelectorAll(".quiz-option-input").forEach((i) => (i.value = ""));
  document.getElementById("quizCorrectIndex").value = 1;
  modal.querySelectorAll(".learning-type:checked").forEach(cb => cb.checked = false);

  const bsModal = bootstrap.Modal.getInstance(modal);
  bsModal.hide();
});


document.getElementById("addPracticeBtn").addEventListener("click", (e) => {
  const modal = e.target.closest(".modal");
  const title = document.getElementById("practiceTitle").value.trim() || "Ćwiczenie praktyczne";
  const code = document.getElementById("practiceCode").value.trim();
  const answer = document.getElementById("practiceAnswer").value.trim();
  const language = document.getElementById("practiceLang").value || "java";

  if (!code || !answer) {
    alert("Wprowadź kod z luką i poprawną odpowiedź!");
    return;
  }

  const selected = [...modal.querySelectorAll(".learning-type:checked")]
    .map(cb => cb.value)
    .join(",") || "0";

  const newBlock = {
    id: Date.now(),
    type: "practice",
    title,
    code,
    answer,
    language,
    learning: selected,
  };

  lessonBlocks.push(newBlock);
  updateLessonPreview();

  document.getElementById("practiceTitle").value = "";
  document.getElementById("practiceCode").value = "";
  document.getElementById("practiceAnswer").value = "";
  document.getElementById("practiceLang").selectedIndex = 0;
  modal.querySelectorAll(".learning-type:checked").forEach(cb => cb.checked = false);

  const bsModal = bootstrap.Modal.getInstance(modal);
  bsModal.hide();
});

document.getElementById("addNoteBtn").addEventListener("click", (e) => {
  const modal = e.target.closest(".modal");
  const content = document.getElementById("noteContent").value.trim();

  if (!content) {
    alert("Wpisz treść notatki!");
    return;
  }

  const selected = [...modal.querySelectorAll(".learning-type:checked")]
    .map(cb => cb.value)
    .join(",") || "0";

  const newBlock = {
    id: Date.now(),
    type: "note",
    content,
    learning: selected,
  };

  lessonBlocks.push(newBlock);
  updateLessonPreview();

  document.getElementById("noteContent").value = "";
  modal.querySelectorAll(".learning-type:checked").forEach(cb => cb.checked = false);
  const bsModal = bootstrap.Modal.getInstance(modal);
  bsModal.hide();
});


document.getElementById("addTipBtn").addEventListener("click", (e) => {
  const modal = e.target.closest(".modal");
  const content = document.getElementById("tipContent").value.trim();

  if (!content) {
    alert("Wpisz treść wskazówki!");
    return;
  }

  const selected = [...modal.querySelectorAll(".learning-type:checked")]
    .map(cb => cb.value)
    .join(",") || "0";

  const newBlock = {
    id: Date.now(),
    type: "tip",
    content,
    learning: selected,
  };

  lessonBlocks.push(newBlock);
  updateLessonPreview();

  document.getElementById("tipContent").value = "";
  modal.querySelectorAll(".learning-type:checked").forEach(cb => cb.checked = false);
  const bsModal = bootstrap.Modal.getInstance(modal);
  bsModal.hide();
});


document.getElementById("addIdeaBtn").addEventListener("click", (e) => {
  const modal = e.target.closest(".modal");
  const content = document.getElementById("ideaContent").value.trim();

  if (!content) {
    alert("Wpisz treść pomysłu!");
    return;
  }

  const selected = [...modal.querySelectorAll(".learning-type:checked")]
    .map(cb => cb.value)
    .join(",") || "0";

  const newBlock = {
    id: Date.now(),
    type: "idea",
    content,
    learning: selected,
  };

  lessonBlocks.push(newBlock);
  updateLessonPreview();

  document.getElementById("ideaContent").value = "";
  modal.querySelectorAll(".learning-type:checked").forEach(cb => cb.checked = false);
  const bsModal = bootstrap.Modal.getInstance(modal);
  bsModal.hide();
});

document.getElementById("addFactBtn").addEventListener("click", (e) => {
  const modal = e.target.closest(".modal");
  const content = document.getElementById("factContent").value.trim();

  if (!content) {
    alert("Wpisz treść ciekawostki!");
    return;
  }

  const selected = [...modal.querySelectorAll(".learning-type:checked")]
    .map(cb => cb.value)
    .join(",") || "0";

  const newBlock = {
    id: Date.now(),
    type: "fact",
    content,
    learning: selected,
  };

  lessonBlocks.push(newBlock);
  updateLessonPreview();

  document.getElementById("factContent").value = "";
  modal.querySelectorAll(".learning-type:checked").forEach(cb => cb.checked = false);
  const bsModal = bootstrap.Modal.getInstance(modal);
  bsModal.hide();
});


document.getElementById("addWarningBtn").addEventListener("click", (e) => {
  const modal = e.target.closest(".modal");
  const content = document.getElementById("warningContent").value.trim();

  if (!content) {
    alert("Wpisz treść ostrzeżenia!");
    return;
  }

  const selected = [...modal.querySelectorAll(".learning-type:checked")]
    .map(cb => cb.value)
    .join(",") || "0";

  const newBlock = {
    id: Date.now(),
    type: "warning",
    content,
    learning: selected,
  };

  lessonBlocks.push(newBlock);
  updateLessonPreview();

  document.getElementById("warningContent").value = "";
  modal.querySelectorAll(".learning-type:checked").forEach(cb => cb.checked = false);
  const bsModal = bootstrap.Modal.getInstance(modal);
  bsModal.hide();
});

document.getElementById("addCodeBtn").addEventListener("click", (e) => {
  const modal = e.target.closest(".modal");
  const title = document.getElementById("codeTitle").value.trim() || "Przykład kodu";
  const language = document.getElementById("codeLanguage").value || "text";
  const content = document.getElementById("codeContent").value.trim();

  if (!content) {
    alert("Wpisz lub wklej kod przed dodaniem!");
    return;
  }

  const selected = [...modal.querySelectorAll(".learning-type:checked")]
    .map(cb => cb.value)
    .join(",") || "0";

  const newBlock = {
    id: Date.now(),
    type: "code",
    title,
    language,
    code: content,
    learning: selected,
  };

  lessonBlocks.push(newBlock);
  updateLessonPreview();


  document.getElementById("codeTitle").value = "";
  document.getElementById("codeLanguage").selectedIndex = 0;
  document.getElementById("codeContent").value = "";
  modal.querySelectorAll(".learning-type:checked").forEach(cb => cb.checked = false);

  const bsModal = bootstrap.Modal.getInstance(modal);
  bsModal.hide();

  console.log("Bloki lekcji:", lessonBlocks);
});



const pendingAudioFiles = [];


document.getElementById("addAudioBtn").addEventListener("click", (e) => {
    const modal = e.target.closest(".modal");
    const fileInput = document.getElementById("audioFile");
    const file = fileInput.files[0];

    if (!file) {
        alert("Wybierz plik MP3!");
        return;
    }

    const ext = file.name.split(".").pop().toLowerCase();
    if (ext !== "mp3") {
        alert("Dozwolone są tylko pliki MP3!");
        return;
    }


    const selected = [...modal.querySelectorAll(".learning-type:checked")]
        .map(cb => cb.value)
        .join(",") || "0";


    const uniqueName = `${crypto.randomUUID()}-${file.name}`;


    const previewUrl = URL.createObjectURL(file);


    Object.defineProperty(file, "name", { value: uniqueName });


    pendingAudioFiles.push(file);


    const newBlock = {
        id: Date.now(),
        type: "audio",
        src: previewUrl,
        title: uniqueName,
        tempFileIndex: pendingAudioFiles.length - 1,
        learning: selected,
    };


    lessonBlocks.push(newBlock);
    updateLessonPreview();


    fileInput.value = "";
    document.getElementById("audioPreview").classList.add("d-none");
    modal.querySelectorAll(".learning-type:checked").forEach(cb => cb.checked = false);
    const bsModal = bootstrap.Modal.getInstance(modal);
    bsModal.hide();

    console.log("🎵 Dodano audio:", uniqueName, "(", file.size, "B )");
});





function generateLessonHTML() {
  const html = lessonBlocks.map(generateBlockHTML).join("\n");

  console.log("📦 Wygenerowany HTML lekcji:\n", html);
  return html;
}


document.getElementById("previewLessonHTMLBtn")?.addEventListener("click", () => {
  const html = generateLessonHTML();


  const newWindow = window.open("", "_blank");
  newWindow.document.write(`
    <html>
      <head>
        <title>Podgląd wygenerowanego HTML</title>
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
        <style>
          body { padding: 1rem; background: #f8f9fa; }
          pre { background: #fff; border-radius: 8px; padding: 1rem; border: 1px solid #ddd; }
        </style>
      </head>
      <body>
        <h4>Wygenerowany HTML lekcji</h4>
        <pre>${escapeHtml(html)}</pre>
      </body>
    </html>
  `);
});



document.getElementById("saveLessonBtn").addEventListener("click", saveLesson);

async function saveLesson() {
    const courseId = document.getElementById("courseId").value;
    const courseSlug = document.getElementById("courseName").value;
    const lessonTitle = document.getElementById("lessonName").value;
    let html = generateLessonHTML();

    lessonBlocks
        .filter(b => b.type === "image")
        .forEach(b => {
            const f = pendingImageFiles[b.tempFileIndex];
            if (f && b.src && b.src.startsWith("blob:")) {
                html = html.replaceAll(b.src, f.name);
            }
        });

    lessonBlocks
        .filter(b => b.type === "audio")
        .forEach(b => {
            const f = pendingAudioFiles[b.tempFileIndex];
            if (f && b.src && b.src.startsWith("blob:")) {
                html = html.replaceAll(b.src, f.name);
            }
        });

    const formData = new FormData();
    formData.append("courseId", courseId);
    formData.append("title", lessonTitle);
    formData.append("contentHtml", html);

    pendingImageFiles.forEach((file) => {
        formData.append("images", file);
        formData.append("imageNames", file.name);
    });

    pendingAudioFiles.forEach((file) => {
        formData.append("audio", file);
        formData.append("audioNames", file.name);
    });

    try {
        const res = await fetch("/api/lessons/save", {
            method: "POST",
            body: formData,
        });

        if (!res.ok) throw new Error("Błąd podczas zapisu lekcji");
        sessionStorage.setItem("successMessage", "Lekcja została utworzona pomyślnie!");
        window.location.href = `/kurs/${courseSlug}`;
    } catch (err) {
        console.error(err);
    }
}


