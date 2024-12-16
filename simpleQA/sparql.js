window.addEventListener('load', () => {
    const endpoint = "https://lod.hozo.jp/kz-fuseki/dis/sparql";

    const sendButton = document.getElementById('send');
    const resultArea = document.getElementById('result_div');
    const symptomArea = document.getElementById('symptom_area');
    const symptomDisplayArea = document.createElement('div'); // 症状表示エリア
    symptomDisplayArea.id = "symptom_display_area";
    document.body.insertBefore(symptomDisplayArea, resultArea.parentElement);

    let filteredDiseases = []; // 現在の疾患候補
    let usedSymptoms = []; // 使用された症状

    sendButton.addEventListener('click', async () => {
        const initialSymptom = document.getElementById('INPUT').value.trim();
        if (initialSymptom && !usedSymptoms.includes(initialSymptom)) {
            addSymptom(initialSymptom); // 入力症状を表示
            usedSymptoms.push(initialSymptom);
            await updateDiseasesAndSymptoms();
        }
    });

    // 症状を追加して表示する関数
    function addSymptom(symptom) {
        const symptomItem = document.createElement('div');
        symptomItem.textContent = symptom;

        // 取り消しボタンを追加
        const removeButton = document.createElement('button');
        removeButton.textContent = "取り消し";
        removeButton.onclick = async () => {
            usedSymptoms = usedSymptoms.filter(s => s !== symptom); // 症状を削除
            symptomDisplayArea.removeChild(symptomItem);
            await updateDiseasesAndSymptoms(); // 状態を更新
        };

        symptomItem.appendChild(removeButton);
        symptomDisplayArea.appendChild(symptomItem);
    }

    // 疾患と症状ボタンを更新する関数
    async function updateDiseasesAndSymptoms() {
        if (usedSymptoms.length === 0) {
            // 全ての症状が取り消された場合、リセット
            clearElement(resultArea);
            clearElement(symptomArea);
            filteredDiseases = [];
            return;
        }

        // AND条件のクエリを動的に構築
        const symptomConditions = usedSymptoms.map((symptom, index) => `
            ?s dis_p:sym ?o${index} .
            ?o${index} rdfs:label "${symptom}"@ja .
        `).join('\n');

        const query = `
            PREFIX dis_p: <https://hozo.jp/dis/prop/> 
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
            SELECT DISTINCT ?s ?sLabel 
            WHERE { 
                ${symptomConditions} 
                ?s rdfs:label ?sLabel . 
            }
        `;

        clearElement(resultArea);
        clearElement(symptomArea);

        try {
            const result = await sendQuery(endpoint, query);
            if (!result.ok) {
                resultArea.innerText = "クエリが失敗しました。";
                return;
            }

            const resultData = await result.json();
            filteredDiseases = mergeDiseasesByUri(resultData.results.bindings);

            displayDiseases(filteredDiseases);
            if (filteredDiseases.length > 1) {
                await showSymptomsForDiseases(filteredDiseases, usedSymptoms);
            }
        } catch (error) {
            resultArea.innerHTML = error.message;
        }
    }

    // 疾患候補を表示する関数
    function displayDiseases(diseases) {
        clearElement(resultArea);

        const diseaseList = document.createElement('ul');
        diseases.forEach(disease => {
            const listItem = document.createElement('li');
            listItem.textContent = disease.label;
            diseaseList.appendChild(listItem);
        });

        resultArea.appendChild(diseaseList);
    }

    // 症状ボタンを表示する関数
    async function showSymptomsForDiseases(diseases, usedSymptoms) {
        const symptomsMap = new Map();

        for (const disease of diseases) {
            const query = `
            PREFIX dis_p: <https://hozo.jp/dis/prop/> 
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
            SELECT DISTINCT ?symptom ?symptomLabel 
            WHERE { 
                <${disease.uri}> dis_p:sym ?symptom . 
                ?symptom rdfs:label ?symptomLabel . 
            }`;

            try {
                const result = await sendQuery(endpoint, query);
                if (!result.ok) continue;

                const resultData = await result.json();
                resultData.results.bindings.forEach(item => {
                    const symptomLabel = item['symptomLabel'].value;
                    if (!usedSymptoms.includes(symptomLabel)) {
                        if (!symptomsMap.has(symptomLabel)) {
                            symptomsMap.set(symptomLabel, []);
                        }
                        symptomsMap.get(symptomLabel).push(disease.label);
                    }
                });
            } catch (error) {
                console.error("症状取得エラー:", error);
            }
        }

        clearElement(symptomArea);

        symptomsMap.forEach((_, symptom) => {
            const button = document.createElement('button');
            button.textContent = symptom;
            button.className = 'symptom-button';
            button.onclick = async () => {
                if (!usedSymptoms.includes(symptom)) {
                    addSymptom(symptom); // ボタンで選択した症状を表示
                    usedSymptoms.push(symptom);
                    await updateDiseasesAndSymptoms(); // 状態を更新
                }
            };
            symptomArea.appendChild(button);
        });
    }

    // 疾患ラベルを統合する関数
    function mergeDiseasesByUri(bindings) {
        const merged = new Map();

        bindings.forEach(item => {
            const uri = item['s'].value;
            const label = item['sLabel'].value;

            if (!merged.has(uri)) {
                merged.set(uri, { uri, labels: [] });
            }
            merged.get(uri).labels.push(label);
        });

        return Array.from(merged.values()).map(disease => ({
            uri: disease.uri,
            label: disease.labels.join(', '),
        }));
    }

    // 要素をクリアする関数
    function clearElement(element) {
        while (element.firstChild) {
            element.removeChild(element.firstChild);
        }
    }

    // SPARQLクエリを送信する関数
    async function sendQuery(endpoint, query) {
        const response = await fetch(`${endpoint}?${new URLSearchParams({ query, output: 'json' })}`);
        return response;
    }
});
