package com.example.h3server.services;

import com.example.h3server.exception.CustomException;
import com.example.h3server.models.FamilyMember;
import com.example.h3server.models.FamilyTree;
import com.example.h3server.repositories.FamilyMemberRepository;
import com.example.h3server.repositories.FamilyTreeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FamilyMemberService {

    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyTreeRepository familyTreeRepository;

    public FamilyMemberService(FamilyMemberRepository familyMemberRepository,
                               FamilyTreeRepository familyTreeRepository) {
        this.familyMemberRepository = familyMemberRepository;
        this.familyTreeRepository = familyTreeRepository;
    }

    public List<FamilyMember> getMembers(Long treeId, String username) {
        FamilyTree familyTree = getTreeOrThrowException(treeId);

        if (familyTree.getIsPrivate() && !familyTree.getUser().getUsername().equals(username)) {
            throw new CustomException("The family tree doesn't exist", HttpStatus.NOT_FOUND);
        }

        return familyMemberRepository.findAllByFamilyTreeId(treeId);
    }

    public FamilyMember addMember(Long treeId, FamilyMember familyMember, String username) {
        FamilyTree familyTree = getTreeOrThrowException(treeId);

        if (!familyTree.getUser().getUsername().equals(username)) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }

        familyMember.setId(null);

        familyMember.setFather(findParent(familyMember.getFather().getId(), familyTree, true));
        familyMember.setMother(findParent(familyMember.getMother().getId(), familyTree, false));

        familyTree.addFamilyMember(familyMember);
        // TODO throw exception if familyMember is invalid
        return familyMemberRepository.save(familyMember);
    }

    public FamilyMember updateMember(Long treeId, Long memberId, FamilyMember familyMember, String username) {
        FamilyTree familyTree = getTreeOrThrowException(treeId);

        if (!familyTree.getUser().getUsername().equals(username)) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }

        FamilyMember memberFromDb = familyTree.getFamilyMember(memberId);
        if (memberFromDb == null) {
            throw new CustomException("The family member doesn't exist", HttpStatus.NOT_FOUND);
        }

        memberFromDb.setFather(findParent(familyMember.getFather().getId(), familyTree, true));
        memberFromDb.setMother(findParent(familyMember.getMother().getId(), familyTree, false));

        memberFromDb.setFirstName(familyMember.getFirstName());
        memberFromDb.setLastName(familyMember.getLastName());
        memberFromDb.setBirthday(familyMember.getBirthday());
        memberFromDb.setDateOfDeath(familyMember.getDateOfDeath());

        // TODO throw exception if memberFromDb is invalid
        return familyMemberRepository.save(memberFromDb);
    }

    private FamilyMember findParent(Long parentId, FamilyTree familyTree, Boolean isFather) {
        if (parentId == null) {
            return null;
        }

        String errorMessage = "Invalid " + (isFather ? "father" : "mother") + " id";

        FamilyMember parent = familyTree.getFamilyMember(parentId);

        if (parent == null) {
            throw new CustomException(errorMessage, HttpStatus.NOT_FOUND);
        }

        return parent;
    }

    private FamilyTree getTreeOrThrowException(Long treeId) {
        return familyTreeRepository.findById(treeId)
                .orElseThrow(() -> new CustomException("The family tree doesn't exist", HttpStatus.NOT_FOUND));
    }
}